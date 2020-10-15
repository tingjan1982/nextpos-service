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

    private TaxableAmount taxableAmount;

    private Map<PlanPeriod, PlanPrice> planPrices = new HashMap<>();

    private List<String> restrictedFeatures = new ArrayList<>();

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
        return planPrices.get(planPeriod);
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
