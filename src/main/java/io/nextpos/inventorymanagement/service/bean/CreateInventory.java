package io.nextpos.inventorymanagement.service.bean;

import io.nextpos.inventorymanagement.data.Inventory;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CreateInventory {

    private String clientId;

    private String productId;

    private List<Inventory.InventoryQuantity> inventoryQuantities;
}
