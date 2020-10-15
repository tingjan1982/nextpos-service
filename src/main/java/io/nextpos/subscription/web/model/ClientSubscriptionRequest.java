package io.nextpos.subscription.web.model;

import io.nextpos.subscription.data.SubscriptionPlan;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ClientSubscriptionRequest {

    private String subscriptionPlanId;

    private SubscriptionPlan.PlanPeriod planPeriod;
}
