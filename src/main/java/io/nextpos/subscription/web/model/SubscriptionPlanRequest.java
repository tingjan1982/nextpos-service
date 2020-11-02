package io.nextpos.subscription.web.model;

import io.nextpos.subscription.data.SubscriptionPlan;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class SubscriptionPlanRequest {

    @Size(min = 2, max = 2)
    @NotBlank
    private String countryCode;

    private SubscriptionPlan.PlanGroup PlanGroup;

    @NotBlank
    private String planName;

    private Map<SubscriptionPlan.PlanPeriod, BigDecimal> planPrices;

    private int userLimit;

    private int deviceLimit;

    private List<String> restrictedFeatures;
}
