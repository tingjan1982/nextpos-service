package io.nextpos.inventorymanagement.service;

import io.nextpos.inventorymanagement.data.Inventory;
import io.nextpos.inventorymanagement.data.InventoryOrder;
import io.nextpos.inventorymanagement.data.InventoryTransaction;
import io.nextpos.inventorymanagement.data.Supplier;
import io.nextpos.inventorymanagement.service.bean.CreateInventory;

import java.util.Optional;

public interface InventoryService {

    Inventory createStock(CreateInventory createInventory);

    Inventory saveInventory(Inventory inventory);

    Inventory getInventory(String id);

    Inventory getInventoryByProductIdOrThrows(String clientId, String productId);

    Optional<Inventory> getInventoryByProductId(String clientId, String productId);

    void deleteInventory(Inventory inventory);

    Supplier saveSupplier(Supplier supplier);

    Supplier getSupplier(String id);

    InventoryOrder saveInventoryOrder(InventoryOrder inventoryOrder);

    InventoryOrder getInventoryOrder(String id);

    void processInventoryOrder(InventoryOrder inventoryOrder);

    InventoryTransaction saveInventoryTransaction(InventoryTransaction inventoryTransaction);

    void processInventoryTransaction(InventoryTransaction inventoryTransaction);
}
