package io.nextpos.ordermanagement.data;

import io.nextpos.shared.model.MongoBaseObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class OrderIdCounter extends MongoBaseObject {

    @Id
    private String id;

    private String orderIdPrefix;

    private int counter;

    public String getOrderId() {
        return orderIdPrefix + "-" + counter;
    }

    @Override
    public boolean isNew() {
        return id == null;
    }
}
