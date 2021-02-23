package io.nextpos.subscription.web.model;

import io.nextpos.subscription.data.SubscriptionPlan;
import io.nextpos.subscription.service.bean.CreateClientSubscription;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class ClientSubscriptionRequest {

    private String clientId;

    @NotBlank
    private String subscriptionPlanId;

    @NotNull
    private SubscriptionPlan.PlanPeriod planPeriod;

    private BigDecimal discountAmount = BigDecimal.ZERO;

    public CreateClientSubscription toCreateClientSubscription() {
        return new CreateClientSubscription(subscriptionPlanId, planPeriod, discountAmount);
    }
}
