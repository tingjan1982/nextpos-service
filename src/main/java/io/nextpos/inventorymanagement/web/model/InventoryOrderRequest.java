package io.nextpos.inventorymanagement.web.model;

import io.nextpos.inventorymanagement.data.Inventory;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
public class InventoryOrderRequest {

    private Date orderDate;

    private String supplierId;

    private String supplierOrderId;

    @NotNull
    private List<InventoryOrderItemRequest> items;

    @Data
    @NoArgsConstructor
    public static class InventoryOrderItemRequest {

        private String inventoryId;

        private Inventory.InventoryQuantity inventoryQuantity;

        private BigDecimal unitPrice;
    }
}
