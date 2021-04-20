package io.nextpos.inventorymanagement.data;

import io.nextpos.shared.model.MongoBaseObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.List;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
public class BillOfMaterial extends MongoBaseObject {

    private String id;

    private String clientId;

    private List<BillOfMaterialItem> materials;


    @Data
    public static class BillOfMaterialItem {

        private String inventoryId;

        private Inventory.UnitOfMeasure unitOfMeasure;

        private BigDecimal quantity;
    }
}
