package io.nextpos.inventorymanagement.web.model;

import io.nextpos.inventorymanagement.data.Inventory;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
public class UpdateInventoryRequest {

    @NotBlank
    private String sku;

    private BigDecimal minimumStockLevel;

    @NotNull
    private List<Inventory.InventoryQuantity> quantities;
}
