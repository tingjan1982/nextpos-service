package io.nextpos.inventorymanagement.data;

import io.nextpos.inventorymanagement.service.bean.CreateInventory;
import io.nextpos.shared.model.MongoBaseObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Inventory extends MongoBaseObject {

    @Id
    private String id;

    private String clientId;

    private String productId;

    private InventoryType inventoryType;

    private Map<String, InventoryQuantity> inventoryQuantities = new HashMap<>();

    @DBRef
    private BillOfMaterial billOfMaterial;

    @Version
    private Long version;

    public Inventory(String clientId, String productId, InventoryType inventoryType) {
        this.clientId = clientId;
        this.productId = productId;
        this.inventoryType = inventoryType;
    }

    public static Inventory createStock(CreateInventory createInventory) {

        final Inventory inventory = new Inventory(createInventory.getClientId(), createInventory.getProductId(), InventoryType.STOCK);
        createInventory.getInventoryQuantities().forEach(inventory::replaceInventoryQuantity);

        return inventory;
    }

    public void replaceInventoryQuantity(InventoryQuantity inventoryQuantity) {

        inventoryQuantities.put(inventoryQuantity.getSku(), inventoryQuantity);
    }

    public void updateInventoryQuantity(String sku, BigDecimal quantity) {

        final InventoryQuantity valueInMap = inventoryQuantities.putIfAbsent(sku, InventoryQuantity.each(sku, quantity));

        if (valueInMap != null) {
            valueInMap.incrementQuantity(quantity);
        }
    }

    public void removeInventoryQuantity(String sku) {
        inventoryQuantities.remove(sku);
    }

    public InventoryQuantity getInventoryQuantity(String sku) {
        return inventoryQuantities.get(sku);
    }

    public double deduceTotalBaseQuantity() {

        return inventoryQuantities.values().stream()
                .mapToDouble(q -> q.getQuantity().multiply(BigDecimal.valueOf(q.baseUnitQuantity)).doubleValue())
                .sum();
    }

    @Data
    @AllArgsConstructor
    public static class InventoryQuantity {

        private String sku;

        private String name;

        private String unitOfMeasure;

        /**
         * 1 if UOM is EACH, else depends on the collective quantity in the defined UOM.
         */
        private int baseUnitQuantity;

        /**
         * Quantity of the inventory in the defined UOM.
         */
        private BigDecimal quantity;

        private BigDecimal minimumStockLevel;

        public static InventoryQuantity each(String sku, BigDecimal quantity) {
            return new InventoryQuantity(sku, sku, UnitOfMeasure.EACH.name(), 1, quantity, BigDecimal.ZERO);
        }

        public void incrementQuantity(BigDecimal quantityToIncrement) {
            this.quantity = this.quantity.add(quantityToIncrement);
        }
    }

    public enum InventoryType {

        /**
         * Material that forms a final product.
         */
        INVENTORY,

        /**
         * Final product that is sold to the customer.
         */
        STOCK
    }

    public enum UnitOfMeasure {

        EACH,

        PACK
    }


}
