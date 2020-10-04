package io.nextpos.ordermanagement.data;

import io.nextpos.shared.model.MongoBaseObject;
import io.nextpos.shared.model.WithClientId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class OrderSet extends MongoBaseObject implements WithClientId {

    @Id
    private String id;

    private String clientId;

    private List<String> linkedOrders;

    private String mainOrderId;

    private OrderSetStatus status;

    public OrderSet(String clientId, List<String> linkedOrders) {
        this.clientId = clientId;
        this.linkedOrders = linkedOrders;
        this.status = OrderSetStatus.OPEN;

        mainOrderId = linkedOrders.iterator().next();
    }

    public enum OrderSetStatus {

        /**
         * Initial state
         */
        OPEN,

        /**
         * Orders are merged and cannot be deleted.
         */
        MERGED,

        /**
         * OrderSet orders are settled.
         */
        COMPLETED
    }
}
