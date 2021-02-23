package io.nextpos.subscription.service.bean;

import io.nextpos.subscription.data.SubscriptionPlan;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class CreateClientSubscription {

    private String subscriptionPlanId;

    private SubscriptionPlan.PlanPeriod planPeriod;

    private BigDecimal discountAmount;
}
