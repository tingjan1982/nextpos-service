package io.nextpos.ordermanagement.data;

import io.nextpos.shared.model.MongoBaseObject;
import io.nextpos.shared.model.WithClientId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class OrderIdCounter extends MongoBaseObject implements WithClientId {

    @Id
    private String id;

    private String clientId;

    private String orderIdPrefix;

    private int counter;

    public OrderIdCounter(String clientId, String orderIdPrefix, int counter) {
        this.clientId = clientId;
        this.orderIdPrefix = orderIdPrefix;
        this.counter = counter;
    }

    public String getOrderId() {
        return orderIdPrefix + "-" + counter;
    }


    @Data
    public static class OrderCounterSummary {

        private String id;

        private String clientId;

        private String orderIdPrefix;

        private int orderCount;
    }
}
