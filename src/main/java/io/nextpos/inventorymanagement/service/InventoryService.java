package io.nextpos.inventorymanagement.service;

import io.nextpos.inventorymanagement.data.Inventory;
import io.nextpos.inventorymanagement.data.InventoryOrder;
import io.nextpos.inventorymanagement.data.InventoryTransaction;
import io.nextpos.inventorymanagement.data.Supplier;

import java.util.Optional;

public interface InventoryService {

    Inventory createStock(String clientId, String sku, Inventory.InventoryQuantity inventoryQuantity);

    Inventory saveInventory(Inventory inventory);

    Inventory getInventory(String id);

    Optional<Inventory> getInventoryBySku(String clientId, String sku);

    void deleteInventory(Inventory inventory);

    Supplier saveSupplier(Supplier supplier);

    Supplier getSupplier(String id);

    InventoryOrder saveInventoryOrder(InventoryOrder inventoryOrder);

    void processInventoryOrder(InventoryOrder inventoryOrder);

    InventoryTransaction saveInventoryTransaction(InventoryTransaction inventoryTransaction);

    void processInventoryTransaction(InventoryTransaction inventoryTransaction);
}
