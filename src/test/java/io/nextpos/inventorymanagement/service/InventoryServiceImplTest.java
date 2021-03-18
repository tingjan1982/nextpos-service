package io.nextpos.inventorymanagement.service;

import io.nextpos.client.data.Client;
import io.nextpos.inventorymanagement.data.Inventory;
import io.nextpos.inventorymanagement.data.InventoryOrder;
import io.nextpos.inventorymanagement.data.InventoryTransaction;
import io.nextpos.inventorymanagement.data.Supplier;
import io.nextpos.inventorymanagement.service.bean.CreateInventory;
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

    private final Client client;

    private final RetryTemplate retryTemplate;

    private Inventory stock;

    @Autowired
    InventoryServiceImplTest(InventoryService inventoryService, Client client) {
        this.inventoryService = inventoryService;
        this.client = client;

        this.retryTemplate = new RetryTemplate();
        this.retryTemplate.setRetryPolicy(new SimpleRetryPolicy());
        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(2000L);
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);
    }

    @BeforeEach
    void prepare() {
        Inventory.InventoryQuantity inventoryQuantity = Inventory.InventoryQuantity.each("2020blue", 10);
        stock = inventoryService.createStock(new CreateInventory(client.getId(), "pid", List.of(inventoryQuantity)));
    }

    @AfterEach
    void cleanup() {
        inventoryService.deleteInventory(inventoryService.getInventory(stock.getId()));
    }

    @Test
    @ChainedTransaction
    void inventoryLifecycle() {

        assertThat(inventoryService.getInventory(stock.getId())).satisfies(i -> {
            assertThat(i.getId()).isNotNull();
            assertThat(i.getInventoryQuantities()).hasSize(1);
        });

        final Supplier supplier = new Supplier(client.getId(), "Alcohol supplier");

        final Supplier.ContactInfo contactInfo = Supplier.ContactInfo.builder()
                .contactPerson("Jimmy")
                .contactEmail("jimmy@email.com")
                .contactNumber("0987654321")
                .contactAddress("Money rd").build();

        supplier.setContactInfo(contactInfo);

        assertThat(inventoryService.saveSupplier(supplier)).satisfies(s -> {
            assertThat(s.getId()).isNotNull();
            assertThat(s.getName()).isNotNull();
            assertThat(s.getContactInfo()).isNotNull();
        });

        assertThat(inventoryService.getSupplier(supplier.getId())).isNotNull();

        final InventoryOrder inventoryOrder = new InventoryOrder(client.getId(), supplier, "oid-12345");
        inventoryOrder.addInventoryOrderItem(stock.getId(), Inventory.InventoryQuantity.each("2020blue", 5), new BigDecimal("100"));
        inventoryService.saveInventoryOrder(inventoryOrder);

        assertThat(inventoryOrder.getId()).isNotNull();
        assertThat(inventoryOrder.getStatus()).isEqualByComparingTo(InventoryOrder.InventoryOrderStatus.PENDING);
        assertThat(inventoryOrder.getInventoryOrderItems()).hasSize(1);

        inventoryService.processInventoryOrder(inventoryOrder);

        assertThat(inventoryService.getInventory(stock.getId())).satisfies(i -> {
            assertThat(i.getInventoryQuantity("2020blue").getQuantity()).isEqualByComparingTo("15");
            assertThat(i.deduceTotalBaseQuantity()).isEqualTo(15);
        });

        final InventoryTransaction inventoryTransaction = new InventoryTransaction(client.getId(), "orderId");
        inventoryTransaction.addInventoryTransactionItem(stock.getId(), "2020blue", 2);
        inventoryService.saveInventoryTransaction(inventoryTransaction);

        assertThat(inventoryTransaction).satisfies(it -> {
            assertThat(it.getId()).isNotNull();
            assertThat(it.getInventoryTransactionItems()).hasSize(1);
        });

        inventoryService.processInventoryTransaction(inventoryTransaction);

        assertThat(inventoryService.getInventory(stock.getId())).satisfies(i -> {
            assertThat(i.getInventoryQuantity("2020blue").getQuantity()).isEqualByComparingTo("13");
            assertThat(i.deduceTotalBaseQuantity()).isEqualTo(13);
        });
    }

    @Test
    void updateInventoryConcurrently() throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<String> task = () -> {
            retryTemplate.execute(r -> {
                final Inventory inventory = inventoryService.getInventory(stock.getId());
                inventory.updateInventoryQuantity(Inventory.InventoryQuantity.each("sku", 1, true));
                return inventoryService.saveInventory(inventory);
            });

            return "";
        };

        executor.invokeAll(List.of(task, task, task));

        assertThat(inventoryService.getInventory(stock.getId())).satisfies(i -> {
            LOGGER.info("{}", i);
            assertThat(i.getInventoryQuantity("sku").getQuantity()).isEqualByComparingTo("7");
        });
    }
}