package io.nextpos.inventorymanagement.service;

import io.nextpos.client.data.Client;
import io.nextpos.inventorymanagement.data.Inventory;
import io.nextpos.inventorymanagement.data.InventoryOrder;
import io.nextpos.inventorymanagement.data.InventoryTransaction;
import io.nextpos.inventorymanagement.data.Supplier;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ChainedTransaction
class InventoryServiceImplTest {

    private final InventoryService inventoryService;

    private final Client client;

    @Autowired
    InventoryServiceImplTest(InventoryService inventoryService, Client client) {
        this.inventoryService = inventoryService;
        this.client = client;
    }

    @Test
    void inventoryLifecycle() {

        Inventory.InventoryQuantity inventoryQuantity = Inventory.InventoryQuantity.each(10);
        final Inventory stock = inventoryService.createStock(client.getId(), "2020blue", inventoryQuantity);

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
        inventoryOrder.addInventoryLineItem(stock.getId(), Inventory.InventoryQuantity.each(5), new BigDecimal("100"));
        inventoryService.saveInventoryOrder(inventoryOrder);

        assertThat(inventoryOrder.getId()).isNotNull();
        assertThat(inventoryOrder.getStatus()).isEqualByComparingTo(InventoryOrder.InventoryOrderStatus.PENDING);
        assertThat(inventoryOrder.getInventoryOrderItems()).hasSize(1);

        inventoryService.processInventoryOrder(inventoryOrder);

        assertThat(inventoryService.getInventory(stock.getId())).satisfies(i -> {
            assertThat(i.getInventoryQuantity(Inventory.UnitOfMeasure.EACH).getQuantity()).isEqualByComparingTo("15");
            assertThat(i.deduceTotalBaseQuantity()).isEqualTo(15);
        });

        final InventoryTransaction inventoryTransaction = new InventoryTransaction(client.getId(), "orderId");
        inventoryTransaction.addInventoryTransactionItem(stock.getId(), 2);
        inventoryService.saveInventoryTransaction(inventoryTransaction);

        assertThat(inventoryTransaction).satisfies(it -> {
            assertThat(it.getId()).isNotNull();
            assertThat(it.getInventoryTransactionItems()).hasSize(1);
        });

        inventoryService.processInventoryTransaction(inventoryTransaction);

        assertThat(inventoryService.getInventory(stock.getId())).satisfies(i -> {
            assertThat(i.getInventoryQuantity(Inventory.UnitOfMeasure.EACH).getQuantity()).isEqualByComparingTo("13");
            assertThat(i.deduceTotalBaseQuantity()).isEqualTo(13);
        });

    }
}