package io.nextpos.inventorymanagement.service.bean;

import io.nextpos.inventorymanagement.data.Inventory;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class CreateInventory {

    private String clientId;

    private String productId;

    private String sku;

    private BigDecimal minimumStockLevel;

    private List<Inventory.InventoryQuantity> quantities;
}
