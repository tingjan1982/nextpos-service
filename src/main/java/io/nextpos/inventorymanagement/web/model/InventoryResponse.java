package io.nextpos.inventorymanagement.web.model;

import io.nextpos.inventorymanagement.data.Inventory;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class InventoryResponse {

    private String id;

    private String productId;

    private Map<String, Inventory.InventoryQuantity> inventoryQuantities;


    public InventoryResponse(Inventory inventory) {
        id = inventory.getId();
        productId = inventory.getProductId();
        inventoryQuantities = inventory.getInventoryQuantities();
    }
}
