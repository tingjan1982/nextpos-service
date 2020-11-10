package io.nextpos.membership.data;

import io.nextpos.calendarevent.data.CalendarEvent;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.shared.model.MongoBaseObject;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

// todo: consider the transaction object more carefully to record enough data to query membership points for future scenarios.
@Document
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class MembershipOrder extends MongoBaseObject {

    @Id
    private String id;

    private String clientId;

    @DBRef
    private Membership membership;

    @DBRef
    private CalendarEvent calendarEvent;

    @DBRef
    private Order order;

    /**
     * Acquired points for this member order.
     */
    private Integer membershipPoints;
}
