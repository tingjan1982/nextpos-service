package io.nextpos.inventorymanagement.service;

import io.nextpos.inventorymanagement.data.Inventory;
import io.nextpos.inventorymanagement.data.InventoryOrder;
import io.nextpos.inventorymanagement.data.InventoryTransaction;
import io.nextpos.inventorymanagement.service.bean.CreateInventory;
import io.nextpos.inventorymanagement.service.bean.InventorySku;

import java.util.List;
import java.util.Optional;

public interface InventoryService {

    Inventory createStock(CreateInventory createInventory);

    Inventory saveInventory(Inventory inventory);

    Inventory getInventory(String id);

    List<InventorySku> searchInventorySkusByKeyword(String clientId, String keyword);

    Inventory getInventoryByProductIdOrThrows(String clientId, String productId);

    Optional<Inventory> getInventoryByProductId(String clientId, String productId);

    void deleteInventory(Inventory inventory);

    InventoryOrder saveInventoryOrder(InventoryOrder inventoryOrder);

    List<InventoryOrder> getInventoryOrders(String clientId);

    InventoryOrder getInventoryOrder(String id);

    InventoryOrder copyInventoryOrder(InventoryOrder inventoryOrder);

    void processInventoryOrder(InventoryOrder inventoryOrder);

    void deleteInventoryOrder(InventoryOrder inventoryOrder);

    InventoryTransaction saveInventoryTransaction(InventoryTransaction inventoryTransaction);

    void processInventoryTransaction(InventoryTransaction inventoryTransaction);
}
