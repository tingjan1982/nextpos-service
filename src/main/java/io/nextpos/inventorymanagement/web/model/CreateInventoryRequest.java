package io.nextpos.inventorymanagement.web.model;

import io.nextpos.inventorymanagement.data.Inventory;
import io.nextpos.inventorymanagement.service.bean.CreateInventory;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
@NoArgsConstructor
public class CreateInventoryRequest {

    @NotBlank
    private String productId;

    private Inventory.InventoryQuantity quantity;

    public CreateInventory toCreateInventory(String clientId) {
        return new CreateInventory(clientId, productId, List.of(quantity));
    }
}
