package io.nextpos.subscription.web.model;

import io.nextpos.subscription.data.ClientSubscription;
import io.nextpos.subscription.data.SubscriptionPlan;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
public class ClientSubscriptionResponse {

    private String id;

    private String planName;

    private ClientSubscription.SubscriptionStatus status;

    private BigDecimal planPrice;

    private SubscriptionPlan.PlanPeriod planPeriod;

    private Date submittedDate;

    private Date planStartDate;

    private Date planEndDate;
}
