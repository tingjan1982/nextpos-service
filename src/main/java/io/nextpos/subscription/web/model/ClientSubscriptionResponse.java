package io.nextpos.subscription.web.model;

import io.nextpos.subscription.data.ClientSubscription;
import io.nextpos.subscription.data.SubscriptionPlan;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@RequiredArgsConstructor
public class ClientSubscriptionResponse {

    private final String id;

    private final String planName;

    private final ClientSubscription.SubscriptionStatus status;

    private final BigDecimal planPrice;

    private final SubscriptionPlan.PlanPeriod planPeriod;

    private final Date submittedDate;

    private final Date planStartDate;

    private final Date planEndDate;

    private String invoiceIdentifier;
}
