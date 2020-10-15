package io.nextpos.subscription.service;

import io.nextpos.settings.data.CountrySettings;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import io.nextpos.subscription.data.SubscriptionPaymentInstruction;
import io.nextpos.subscription.data.SubscriptionPlan;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ChainedTransaction
class SubscriptionPlanServiceImplTest {

    private final SubscriptionPlanService subscriptionPlanService;

    private final CountrySettings countrySettings;

    @Autowired
    SubscriptionPlanServiceImplTest(SubscriptionPlanService subscriptionPlanService, CountrySettings countrySettings) {
        this.subscriptionPlanService = subscriptionPlanService;
        this.countrySettings = countrySettings;
    }

    @Test
    void saveSubscriptionPlan() {

        final SubscriptionPlan subscriptionPlan = new SubscriptionPlan(Locale.TAIWAN.getCountry(), SubscriptionPlan.PlanGroup.DEFAULT, "standardPlan", countrySettings);
        subscriptionPlan.addPlanPrice(SubscriptionPlan.PlanPeriod.MONTHLY, new SubscriptionPlan.PlanPrice(new BigDecimal("1450")));

        subscriptionPlanService.saveSubscriptionPlan(subscriptionPlan);

        final SubscriptionPlan retrievedSubscription = subscriptionPlanService.getSubscription(subscriptionPlan.getId());

        assertThat(retrievedSubscription.getId()).isNotNull();
        assertThat(retrievedSubscription.getPlanPrices()).hasSize(1);
        assertThat(retrievedSubscription.getCountryCode()).hasSize(2);

        assertThat(subscriptionPlanService.getSubscriptionPlans(Locale.TAIWAN.getCountry())).isNotEmpty();
    }

    @Test
    void saveSubscriptionPaymentInstruction() {

        final SubscriptionPaymentInstruction instruction = new SubscriptionPaymentInstruction(Locale.TAIWAN.getCountry(), "d-dd8bd80c86c74ea9a9ff2a96dcfb462d");

        subscriptionPlanService.saveSubscriptionPaymentInstruction(instruction);

        assertThat(instruction.getId()).isNotNull();
    }
}