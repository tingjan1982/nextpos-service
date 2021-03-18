package io.nextpos.inventorymanagement.web.model;

import io.nextpos.inventorymanagement.data.InventoryOrder;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
public class InventoryOrderResponse {

    private String id;

    private String supplierOrderId;

    private Date orderDate;

    private List<InventoryOrder.InventoryOrderItem> items;

    public InventoryOrderResponse(InventoryOrder inventoryOrder) {
        id = inventoryOrder.getId();
        supplierOrderId = inventoryOrder.getSupplierOrderId();
        orderDate = inventoryOrder.getOrderDate();
        items = inventoryOrder.getInventoryOrderItems();
    }
}
