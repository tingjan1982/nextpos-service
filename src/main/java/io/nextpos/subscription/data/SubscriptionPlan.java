package io.nextpos.subscription.data;

import io.nextpos.shared.model.MongoBaseObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One subscription plan for a given country code.
 */
@Document
@Data
@EqualsAndHashCode(callSuper = true)
public class SubscriptionPlan extends MongoBaseObject {

    @Id
    private String id;

    private String countryCode;

    @Enumerated(EnumType.STRING)
    private PlanGroup planGroup;

    private String planName;

    private Map<PlanPeriod, PlanPrice> planPrices = new HashMap<>();

    private List<String> restrictedFeatures = new ArrayList<>();

    public SubscriptionPlan(String countryCode, PlanGroup planGroup, String planName) {
        this.countryCode = countryCode;
        this.planGroup = planGroup;
        this.planName = planName;
    }

    public void addPlanPrice(PlanPeriod planPeriod, PlanPrice planPrice) {
        planPrices.put(planPeriod, planPrice);
    }

    @Data
    @AllArgsConstructor
    public static class PlanPrice {

        private BigDecimal planMonthlyPrice;

    }

    public enum PlanGroup {
        DEFAULT, FOOD_BEVERAGE, RETAIL, RESERVATION
    }

    public enum PlanPeriod {
        MONTHLY, HALF_YEARLY, YEARLY
    }
}
