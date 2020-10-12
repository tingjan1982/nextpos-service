package io.nextpos.subscription.data;

import io.nextpos.shared.model.MongoBaseObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.Date;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
public class ClientSubscription extends MongoBaseObject {

    @Id
    private String id;

    private String clientId;

    @DBRef
    private SubscriptionPlan subscriptionPlan;

    private SubscriptionStatus status;

    private BigDecimal planPrice;

    private SubscriptionPlan.PlanPeriod planPeriod;

    private Date startDate;

    private Date endDate;


    public enum SubscriptionStatus {

        /**
         * This plan is in use. At most one active plan for a client id.
         */
        ACTIVE,

        /**
         * This plan is stopped for whatever reason. (e.g. non payment)
         */
        INACTIVE,

        /**
         * This plan has expired.
         */
        EXPIRED
    }
}
