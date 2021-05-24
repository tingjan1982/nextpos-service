package io.nextpos.inventorymanagement.data;

import io.nextpos.shared.model.MongoBaseObject;
import io.nextpos.shared.model.WithClientId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Object to track inventories that are sold to customers.
 */
@Document
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class InventoryTransaction extends MongoBaseObject implements WithClientId {

    @Id
    private String id;

    private String clientId;

    private String orderId;

    private Date orderDate = new Date();

    private List<InventoryTransactionItem> inventoryTransactionItems = new ArrayList<>();

    private InventoryTransactionStatus status = InventoryTransactionStatus.PENDING;

    private AtomicInteger counter = new AtomicInteger(1);

    public InventoryTransaction(String clientId, String orderId) {
        this.id = ObjectId.get().toString();
        this.clientId = clientId;
        this.orderId = orderId;
    }

    public void addInventoryTransactionItem(String inventoryId, String sku, BigDecimal quantity) {

        String itemId = this.id + "-" + counter.getAndIncrement();
        final InventoryTransactionItem item = new InventoryTransactionItem(itemId, inventoryId, sku, quantity);

        inventoryTransactionItems.add(item);
    }

    public boolean hasInventoryTransactionItems() {
        return !inventoryTransactionItems.isEmpty();
    }

    @Data
    @AllArgsConstructor
    public static class InventoryTransactionItem {

        private String id;

        private String inventoryId;

        private String sku;

        private BigDecimal quantity;
    }


    public enum InventoryTransactionStatus {

        PENDING, PROCESSED
    }
}
