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
import java.util.stream.Collectors;

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

    public void addInventoryOrderItem(String inventoryId, String sku, BigDecimal quantity, BigDecimal unitPrice) {

        String itemId = id + "-" + counter.getAndIncrement();
        final InventoryOrderItem inventoryOrderItem = new InventoryOrderItem(itemId, inventoryId, sku, quantity, unitPrice);
        inventoryOrderItems.add(inventoryOrderItem);

    }

    public InventoryOrder copy() {

        final InventoryOrder copy = new InventoryOrder(clientId, supplier, supplierOrderId);
        copy.id = new ObjectId().toString();
        copy.orderDate = orderDate;
        copy.orderAmount = orderAmount;
        copy.inventoryOrderItems = inventoryOrderItems.stream()
                .map(InventoryOrderItem::copy)
                .peek(i -> i.setId(copy.id + "-" + copy.counter.getAndIncrement()))
                .collect(Collectors.toList());

        return copy;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryOrderItem {

        private String id;

        private String inventoryId;

        private String sku;

        private BigDecimal quantity;

        private BigDecimal unitPrice;

        public InventoryOrderItem copy() {
            final InventoryOrderItem copy = new InventoryOrderItem();
            copy.inventoryId = inventoryId;
            copy.sku = sku;
            copy.quantity = quantity;
            copy.unitPrice = unitPrice;

            return copy;
        }
    }

    public enum InventoryOrderStatus {

        PENDING, PROCESSED
    }
}
