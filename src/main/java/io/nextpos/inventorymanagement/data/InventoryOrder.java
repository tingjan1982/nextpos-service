package io.nextpos.inventorymanagement.data;

import io.nextpos.shared.model.MongoBaseObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Object to track incoming inventory orders provided by supplier.
 */
@Document
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class InventoryOrder extends MongoBaseObject {

    @Id
    private String id;

    private String clientId;

    private Date orderDate = new Date();

    @DBRef
    private Supplier supplier;

    private String supplierOrderId;

    private List<InventoryOrderItem> inventoryOrderItems = new ArrayList<>();

    private BigDecimal orderAmount = BigDecimal.ZERO;

    private InventoryOrderStatus status = InventoryOrderStatus.PENDING;

    private AtomicInteger counter = new AtomicInteger(1);

    public InventoryOrder(String clientId, Supplier supplier, String supplierOrderId) {
        id = ObjectId.get().toString();
        this.clientId = clientId;
        this.supplier = supplier;
        this.supplierOrderId = supplierOrderId;
    }

    public void addInventoryOrderItem(String inventoryId, Inventory.InventoryQuantity inventoryQuantity, BigDecimal unitPrice) {

        String itemId = id + "-" + counter.getAndIncrement();
        final InventoryOrderItem inventoryOrderItem = new InventoryOrderItem(itemId, inventoryId, inventoryQuantity, unitPrice);
        inventoryOrderItems.add(inventoryOrderItem);

    }

    public void removeInventoryLineItem(String itemId) {
        inventoryOrderItems.removeIf(i -> i.id.equals(itemId));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryOrderItem {

        private String id;

        private String inventoryId;

        private Inventory.InventoryQuantity inventoryQuantity;

        private BigDecimal unitPrice;


    }

    public enum InventoryOrderStatus {

        PENDING, PROCESSED
    }
}
