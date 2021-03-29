package io.nextpos.inventorymanagement.web.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
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
    @Valid
    private List<InventoryOrderItemRequest> items;

    @Data
    @NoArgsConstructor
    public static class InventoryOrderItemRequest {

        @NotNull
        private String inventoryId;

        @NotNull
        private String sku;

        @NotNull
        private BigDecimal quantity;

        private BigDecimal unitPrice;
    }
}
