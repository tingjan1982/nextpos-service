package io.nextpos.inventorymanagement.web.model;

import io.nextpos.inventorymanagement.data.Inventory;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@AllArgsConstructor
public class InventoryResponse {

    private String id;

    private String sku;

    private BigDecimal minimumStockLevel;

    private Map<Inventory.UnitOfMeasure, Inventory.InventoryQuantity> inventoryQuantities;


    public InventoryResponse(Inventory inventory) {
        id = inventory.getId();
        sku = inventory.getSku();
        minimumStockLevel = inventory.getMinimumStockLevel();
        inventoryQuantities = inventory.getInventoryQuantities();
    }
}
