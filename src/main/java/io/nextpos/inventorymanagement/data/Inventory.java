package io.nextpos.inventorymanagement.data;

import io.nextpos.shared.model.MongoBaseObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Document
@CompoundIndexes({@CompoundIndex(name = "unique_per_client_index", def = "{'clientId': 1, 'sku': 1}", unique = true)})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Inventory extends MongoBaseObject {

    @Id
    private String id;

    private String clientId;

    /**
     * Can reference to the product.
     */
    private String sku;

    private InventoryType inventoryType;

    private String name;

    private Map<UnitOfMeasure, InventoryQuantity> inventoryQuantities = new HashMap<>();

    private BigDecimal totalQuantity;

    private BigDecimal minimumStockLevel;

    @DBRef
    private BillOfMaterial billOfMaterial;

    public Inventory(String clientId, String sku, InventoryType inventoryType) {
        this.clientId = clientId;
        this.sku = sku;
        this.inventoryType = inventoryType;
    }

    public static Inventory createStock(String clientId, String sku) {
        return new Inventory(clientId, sku, InventoryType.STOCK);
    }

    public static Inventory createInventory(String clientId, String sku) {
        return new Inventory(clientId, sku, InventoryType.INVENTORY);
    }

    public void updateInventoryQuantity(InventoryQuantity inventoryQuantity) {

        final InventoryQuantity valueInMap = inventoryQuantities.putIfAbsent(inventoryQuantity.getUnitOfMeasure(), inventoryQuantity);

        if (valueInMap != null) {
            valueInMap.incrementQuantity(inventoryQuantity);
        }
    }

    public InventoryQuantity getInventoryQuantity(UnitOfMeasure unitOfMeasure) {
        return inventoryQuantities.get(unitOfMeasure);
    }

    public double deduceTotalBaseQuantity() {

        return inventoryQuantities.values().stream()
                .mapToDouble(q -> q.getQuantity().multiply(BigDecimal.valueOf(q.baseUnitQuantity)).doubleValue())
                .sum();
    }

    @Data
    @AllArgsConstructor
    public static class InventoryQuantity {

        private UnitOfMeasure unitOfMeasure;

        /**
         * 1 if UOM is EACH, else depends on the collective quantity in the defined UOM.
         */
        private int baseUnitQuantity;

        /**
         * Quantity of the inventory in the defined UOM.
         */
        private BigDecimal quantity;

        public static InventoryQuantity of(UnitOfMeasure unitOfMeasure, BigDecimal quantity) {
            return new InventoryQuantity(unitOfMeasure, 1, quantity);
        }

        public static InventoryQuantity each(int quantity) {
            return each(quantity, false);
        }

        public static InventoryQuantity each(int quantity, boolean negative) {

            BigDecimal inventoryQty = BigDecimal.valueOf(quantity);

            if (negative) {
                inventoryQty = inventoryQty.negate();
            }

            return new InventoryQuantity(UnitOfMeasure.EACH, 1, inventoryQty);
        }

        public void incrementQuantity(InventoryQuantity quantityToIncrement) {
            this.quantity = this.quantity.add(quantityToIncrement.getQuantity());
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

        PACKAGE, CASE, CARTON,
    }


}
