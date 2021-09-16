package io.nextpos.inventorymanagement.service;

import io.nextpos.client.data.Client;
import io.nextpos.inventorymanagement.data.Inventory;
import io.nextpos.inventorymanagement.data.InventoryOrder;
import io.nextpos.inventorymanagement.data.InventoryTransaction;
import io.nextpos.inventorymanagement.data.Supplier;
import io.nextpos.inventorymanagement.service.bean.CreateInventory;
import io.nextpos.product.data.Product;
import io.nextpos.product.service.ProductService;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class InventoryServiceImplTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryServiceImplTest.class);

    private final InventoryService inventoryService;

    private final ProductService productService;

    private final SupplierService supplierService;

    private final Client client;

    private final RetryTemplate retryTemplate;

    private Inventory stock;

    @Autowired
    InventoryServiceImplTest(InventoryService inventoryService, ProductService productService, SupplierService supplierService, Client client) {
        this.inventoryService = inventoryService;
        this.productService = productService;
        this.supplierService = supplierService;
        this.client = client;

        this.retryTemplate = new RetryTemplate();
        this.retryTemplate.setRetryPolicy(new SimpleRetryPolicy());
        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(2000L);
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);
    }

    @BeforeEach
    void prepare() {

        Product product = Product.builder(client).productNameAndPrice("T-Shirt", BigDecimal.TEN).build();
        productService.saveProduct(product);

        Inventory.InventoryQuantity blue = Inventory.InventoryQuantity.each("2021blue", new BigDecimal(10));
        Inventory.InventoryQuantity red = Inventory.InventoryQuantity.each("2021red", new BigDecimal(10));
        stock = inventoryService.createStock(new CreateInventory(client.getId(), product.getId(), List.of(blue, red)));
    }

    @AfterEach
    void cleanup() {
        inventoryService.deleteInventory(inventoryService.getInventory(stock.getId()));
    }

    @Test
    @ChainedTransaction
    void inventoryLifecycle() {

        assertThat(inventoryService.getInventory(stock.getId())).satisfies(i -> {
            assertThat(i.getProductId()).isNotNull();
            assertThat(i.getProductName()).isNotNull();
            assertThat(i.getInventoryQuantities()).hasSize(2);
        });

        assertThat(inventoryService.searchInventorySkusByKeyword(client.getId(), "Shirt")).hasSize(2);
        assertThat(inventoryService.searchInventorySkusByKeyword(client.getId(), "blue")).hasSize(1);

        final Supplier supplier = new Supplier(client.getId(), "Alcohol supplier");

        final Supplier.ContactInfo contactInfo = Supplier.ContactInfo.builder()
                .contactPerson("Jimmy")
                .contactEmail("jimmy@email.com")
                .contactNumber("0987654321")
                .contactAddress("Money rd").build();

        supplier.setContactInfo(contactInfo);

        assertThat(supplierService.saveSupplier(supplier)).satisfies(s -> {
            assertThat(s.getId()).isNotNull();
            assertThat(s.getName()).isNotNull();
            assertThat(s.getContactInfo()).isNotNull();
        });

        assertThat(supplierService.getSupplier(supplier.getId())).isNotNull();

        final InventoryOrder inventoryOrder = new InventoryOrder(client.getId(), supplier, "oid-12345");
        inventoryOrder.addInventoryOrderItem(stock.getId(), "2021blue", new BigDecimal("5"), new BigDecimal("100"));
        inventoryService.saveInventoryOrder(inventoryOrder);

        assertThat(inventoryService.getInventoryOrders(client.getId())).hasSize(1);

        assertThat(inventoryOrder.getId()).isNotNull();
        assertThat(inventoryOrder.getStatus()).isEqualByComparingTo(InventoryOrder.InventoryOrderStatus.PENDING);
        assertThat(inventoryOrder.getInventoryOrderItems()).hasSize(1);

        inventoryService.processInventoryOrder(inventoryOrder);

        assertThat(inventoryService.getInventory(stock.getId())).satisfies(i -> {
            assertThat(i.getInventoryQuantity("2021blue").getQuantity()).isEqualByComparingTo("15");
        });

        final InventoryTransaction inventoryTransaction = new InventoryTransaction(client.getId(), "orderId");
        inventoryTransaction.addInventoryTransactionItem(stock.getId(), "2021blue", new BigDecimal("2"));
        inventoryService.saveInventoryTransaction(inventoryTransaction);

        assertThat(inventoryTransaction).satisfies(it -> {
            assertThat(it.getId()).isNotNull();
            assertThat(it.getInventoryTransactionItems()).hasSize(1);
        });

        inventoryService.processInventoryTransaction(inventoryTransaction);

        assertThat(inventoryService.getInventory(stock.getId())).satisfies(i -> {
            assertThat(i.getInventoryQuantity("2021blue").getQuantity()).isEqualByComparingTo("13");
        });
    }

    @Test
    void updateInventoryConcurrently() throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<String> task = () -> {
            retryTemplate.execute(r -> {
                final Inventory inventory = inventoryService.getInventory(stock.getId());
                inventory.updateInventoryQuantity("2021blue", new BigDecimal("1"));
                return inventoryService.saveInventory(inventory);
            });

            return "";
        };

        executor.invokeAll(List.of(task, task, task));

        assertThat(inventoryService.getInventory(stock.getId())).satisfies(i -> {
            LOGGER.info("{}", i);
            assertThat(i.getInventoryQuantity("2021blue").getQuantity()).isEqualByComparingTo("13");
        });
    }
}