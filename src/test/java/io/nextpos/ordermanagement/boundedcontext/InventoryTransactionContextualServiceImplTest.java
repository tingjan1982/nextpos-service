package io.nextpos.ordermanagement.boundedcontext;

import io.nextpos.client.data.Client;
import io.nextpos.inventorymanagement.data.Inventory;
import io.nextpos.inventorymanagement.data.InventoryTransaction;
import io.nextpos.inventorymanagement.service.InventoryService;
import io.nextpos.inventorymanagement.service.bean.CreateInventory;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.ProductSnapshot;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.settings.data.CountrySettings;
import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ChainedTransaction
class InventoryTransactionContextualServiceImplTest {

    private final InventoryTransactionContextualService inventoryTransactionContextualService;

    private final InventoryService inventoryService;

    private final OrderService orderService;

    private final Client client;

    private final CountrySettings countrySettings;

    @Autowired
    InventoryTransactionContextualServiceImplTest(InventoryTransactionContextualService inventoryTransactionContextualService, InventoryService inventoryService, OrderService orderService, Client client, CountrySettings countrySettings) {
        this.inventoryTransactionContextualService = inventoryTransactionContextualService;
        this.inventoryService = inventoryService;
        this.orderService = orderService;
        this.client = client;
        this.countrySettings = countrySettings;
    }

    @Test
    void createInventoryTransaction() {

        String sku = "hat001";
        Inventory.InventoryQuantity inventoryQuantity = Inventory.InventoryQuantity.each(100);
        final Inventory stock = inventoryService.createStock(new CreateInventory(client.getId(), "hat", sku, BigDecimal.ZERO, List.of(inventoryQuantity)));

        Order order = Order.newOrder(client.getId(), Order.OrderType.IN_STORE, DummyObjects.orderSettings(countrySettings));
        ProductSnapshot productSnapshot = new ProductSnapshot("hat", "hat", sku, new BigDecimal("100"), List.of());
        order.addOrderLineItem(productSnapshot, 1);
        orderService.saveOrder(order);

        final Optional<InventoryTransaction> inventoryTransaction = inventoryTransactionContextualService.createAndProcessInventoryTransaction(order);

        assertThat(inventoryTransaction).isNotEmpty();
        assertThat(inventoryTransaction.get()).satisfies(it -> {
            assertThat(it.getOrderId()).isEqualTo(order.getId());
            assertThat(it.getInventoryTransactionItems()).hasSize(1);
        });

        assertThat(inventoryService.getInventory(stock.getId())).satisfies(i -> {
            assertThat(i.getInventoryQuantity(Inventory.UnitOfMeasure.EACH).getQuantity()).isEqualByComparingTo("99");
        });
    }

    @Test
    void createInventoryTransaction_InventoryNotConfigured() {

        Order order = Order.newOrder(client.getId(), Order.OrderType.IN_STORE, DummyObjects.orderSettings(countrySettings));
        ProductSnapshot productSnapshot = new ProductSnapshot("hat", "hat", null, new BigDecimal("100"), List.of());
        order.addOrderLineItem(productSnapshot, 1);
        orderService.saveOrder(order);

        final Optional<InventoryTransaction> inventoryTransaction = inventoryTransactionContextualService.createAndProcessInventoryTransaction(order);

        assertThat(inventoryTransaction).isEmpty();
    }
}