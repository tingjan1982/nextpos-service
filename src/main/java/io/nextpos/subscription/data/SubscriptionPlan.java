package io.nextpos.subscription.data;

import io.nextpos.ordermanagement.data.TaxableAmount;
import io.nextpos.settings.data.CountrySettings;
import io.nextpos.shared.model.MongoBaseObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
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
@NoArgsConstructor
public class SubscriptionPlan extends MongoBaseObject {

    @Id
    private String id;

    private String countryCode;

    @Enumerated(EnumType.STRING)
    private PlanGroup planGroup;

    private String planName;

    private String description;

    private TaxableAmount taxableAmount;

    private Map<PlanPeriod, PlanPrice> planPrices = new HashMap<>();

    private SubscriptionPlanLimit subscriptionLimit = new SubscriptionPlanLimit();

    public SubscriptionPlan(String countryCode, PlanGroup planGroup, String planName, CountrySettings countrySettings) {
        this.countryCode = countryCode;
        this.planGroup = planGroup;
        this.planName = planName;
        this.taxableAmount = new TaxableAmount(countrySettings.getTaxRate(), countrySettings.getTaxInclusive());
    }

    public void addPlanPrice(PlanPeriod planPeriod, PlanPrice planPrice) {
        planPrices.put(planPeriod, planPrice);
    }

    public PlanPrice getPlanPrice(PlanPeriod planPeriod) {
        PlanPrice planPrice = planPrices.get(planPeriod);

        if (planPrice == null) {
            planPrice = planPrices.get(PlanPeriod.MONTHLY);
        }

        return planPrice;
    }

    public void updateSubscriptionLimit(int userLimit, int deviceLimit, List<String> restrictedFeatures) {
        subscriptionLimit.setUserLimit(userLimit);
        subscriptionLimit.setDeviceLimit(deviceLimit);
        subscriptionLimit.setRestrictedFeatures(restrictedFeatures);
    }

    public int getUserLimit() {
        return subscriptionLimit.getUserLimit();
    }

    public int getDeviceLimit() {
        return subscriptionLimit.getDeviceLimit();
    }

    @Data
    @AllArgsConstructor
    public static class PlanPrice {

        private BigDecimal planMonthlyPrice;

    }

    @Data
    public static class SubscriptionPlanLimit {

        /**
         * 0 indicate unlimited.
         */
        private int deviceLimit;

        /**
         * 0 indicate unlimited.
         */
        private int userLimit;

        private List<String> restrictedFeatures = new ArrayList<>();
    }

    public enum PlanGroup {
        DEFAULT, FOOD_BEVERAGE, RETAIL, RESERVATION
    }

    public enum PlanPeriod {
        MONTHLY(1),
        QUARTERLY(3),
        HALF_YEARLY(6),
        YEARLY(12);

        private final int numberOfMonths;

        PlanPeriod(int numberOfMonths) {
            this.numberOfMonths = numberOfMonths;
        }

        public int getNumberOfMonths() {
            return numberOfMonths;
        }
    }
}
