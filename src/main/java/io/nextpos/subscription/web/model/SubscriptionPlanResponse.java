package io.nextpos.subscription.web.model;

import io.nextpos.subscription.data.SubscriptionPlan;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class SubscriptionPlanResponse {

    private String id;

    private final String countryCode;

    private final SubscriptionPlan.PlanGroup planGroup;

    private final String planName;

    private final String description;

    private final Map<SubscriptionPlan.PlanPeriod, BigDecimal> planPrices;

    private final List<String> restrictedFeatures;

    public SubscriptionPlanResponse(SubscriptionPlan subscriptionPlan) {
        id = subscriptionPlan.getId();
        countryCode = subscriptionPlan.getCountryCode();
        planGroup = subscriptionPlan.getPlanGroup();
        planName = subscriptionPlan.getPlanName();
        description = subscriptionPlan.getDescription();
        planPrices = subscriptionPlan.getPlanPrices().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getAmount()));
        restrictedFeatures = subscriptionPlan.getSubscriptionLimit().getRestrictedFeatures();
    }
}
