package io.nextpos.subscription.data;

import io.nextpos.shared.model.MongoBaseObject;
import io.nextpos.shared.model.WithClientId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.Date;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ClientSubscription extends MongoBaseObject implements WithClientId {

    @Id
    private String id;

    private String clientId;

    private SubscriptionPlan subscriptionPlanSnapshot;

    private SubscriptionStatus status;

    private boolean current;

    private BigDecimal planPrice;

    private SubscriptionPlan.PlanPeriod planPeriod;

    /**
     * Non-recurring discount amount.
     */
    private BigDecimal discountAmount;

    private Date submittedDate;

    /**
     * Overall start date.
     */
    private Date planStartDate;

    /**
     * Overall plan end date.
     */
    private Date planEndDate;

    private String currentInvoiceId;
    

    public ClientSubscription(String clientId, SubscriptionPlan subscriptionPlanSnapshot, SubscriptionPlan.PlanPeriod planPeriod) {
        this(clientId, subscriptionPlanSnapshot, planPeriod, BigDecimal.ZERO);
    }

    public ClientSubscription(String clientId, SubscriptionPlan subscriptionPlanSnapshot, SubscriptionPlan.PlanPeriod planPeriod, BigDecimal discountAmount) {
        this.clientId = clientId;
        this.subscriptionPlanSnapshot = subscriptionPlanSnapshot;
        this.planPeriod = planPeriod;
        this.planPrice = subscriptionPlanSnapshot.getPlanPrice(planPeriod).getPlanMonthlyPrice();
        this.discountAmount = discountAmount;

        this.status = SubscriptionStatus.SUBMITTED;
        this.submittedDate = new Date();
        this.current = true;
    }

    public boolean isActiveSubscription() {
        return this.status.isActive();
    }

    public enum SubscriptionStatus {

        /**
         * Subscription request is sent, not yet received payment.
         */
        SUBMITTED(false),

        /**
         * This plan is in use.
         */
        ACTIVE(true),

        /**
         * Active and issued renewal invoice
         */
        ACTIVE_RENEWING(true),

        /**
         * This plan is active but client decides to not renew in the next cycle.
         */
        ACTIVE_LAPSING(true),

        /**
         * This plan is suspended for whatever reason. (e.g. non payment)
         */
        INACTIVE(false),

        /**
         * This plan has lapsed.
         */
        LAPSED(false),

        /**
         * This plan is cancelled immediately, either by user or admin
         */
        CANCELLED(false);

        private final boolean active;

        SubscriptionStatus(boolean active) {
            this.active = active;
        }

        boolean isActive() {
            return active;
        }
    }
}
