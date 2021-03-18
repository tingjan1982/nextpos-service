package io.nextpos.inventorymanagement.web.model;

import io.nextpos.inventorymanagement.data.Inventory;
import io.nextpos.inventorymanagement.service.bean.CreateInventory;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
public class CreateInventoryRequest {

    @NotBlank
    private String productId;

    @NotBlank
    private String sku;

    private BigDecimal minimumStockLevel;

    private List<Inventory.InventoryQuantity> quantities;

    public CreateInventory toCreateInventory(String clientId) {
        return new CreateInventory(clientId, productId, sku, minimumStockLevel, quantities);
    }
}
