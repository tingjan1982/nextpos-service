package io.nextpos.subscription.data;

import io.nextpos.shared.model.MongoBaseObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
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

    private SubscriptionPlan subscriptionPlanSnapshot;

    private SubscriptionStatus status;

    private BigDecimal planPrice;

    private SubscriptionPlan.PlanPeriod planPeriod;

    private Date submittedDate;

    /**
     * Overall start date.
     */
    private Date planStartDate;

    /**
     * Overall plan end date.
     */
    private Date planEndDate;
    

    public ClientSubscription(String clientId, SubscriptionPlan subscriptionPlanSnapshot, SubscriptionPlan.PlanPeriod planPeriod) {
        this.clientId = clientId;
        this.subscriptionPlanSnapshot = subscriptionPlanSnapshot;
        this.planPeriod = planPeriod;
        this.planPrice = subscriptionPlanSnapshot.getPlanPrice(planPeriod).getPlanMonthlyPrice();

        this.status = SubscriptionStatus.SUBMITTED;
        this.submittedDate = new Date();
    }

    public enum SubscriptionStatus {

        /**
         * Subscription request is sent, not yet received payment.
         */
        SUBMITTED,

        /**
         * This plan is in use. At most one active plan for a client id.
         */
        ACTIVE,

        /**
         * This plan is active but client decides to not renew in the next cycle.
         */
        ACTIVE_LAPSING,

        /**
         * This plan is suspended for whatever reason. (e.g. non payment)
         */
        INACTIVE,

        /**
         * This plan has expired.
         */
        EXPIRED,

        /**
         * This plan is cancelled immediately, either by user or admin
         */
        CANCELLED
    }
}
