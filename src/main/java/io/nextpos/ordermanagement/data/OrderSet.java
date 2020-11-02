package io.nextpos.ordermanagement.data;

import io.nextpos.shared.model.MongoBaseObject;
import io.nextpos.shared.model.WithClientId;
import io.nextpos.tablelayout.data.TableLayout;
import lombok.AllArgsConstructor;
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

    private List<OrderSetDetails> linkedOrders;

    private String mainOrderId;

    private String tableLayoutId;

    private OrderSetStatus status;

    public OrderSet(String clientId, List<OrderSetDetails> linkedOrders, String tableLayoutId) {
        this.clientId = clientId;
        this.linkedOrders = linkedOrders;
        this.tableLayoutId = tableLayoutId;
        this.status = OrderSetStatus.OPEN;

        mainOrderId = linkedOrders.get(0).getOrderId();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderSetDetails {

        private String orderId;

        private String tableName;

        private TableLayout.TableDetails.ScreenPosition screenPosition;
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
