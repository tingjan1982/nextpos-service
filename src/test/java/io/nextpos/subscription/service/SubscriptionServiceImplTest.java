package io.nextpos.subscription.service;

import io.nextpos.shared.service.annotation.ChainedTransaction;
import io.nextpos.subscription.data.SubscriptionPlan;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ChainedTransaction
class SubscriptionServiceImplTest {

    private final SubscriptionService subscriptionService;

    @Autowired
    SubscriptionServiceImplTest(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Test
    void saveSubscriptionPlan() {

        final SubscriptionPlan subscriptionPlan = new SubscriptionPlan(Locale.TAIWAN.getCountry(), SubscriptionPlan.PlanGroup.DEFAULT, "freePlan");
        subscriptionPlan.addPlanPrice(SubscriptionPlan.PlanPeriod.MONTHLY, new SubscriptionPlan.PlanPrice(new BigDecimal("1450")));

        subscriptionService.saveSubscriptionPlan(subscriptionPlan);

        final SubscriptionPlan retrievedSubscription = subscriptionService.getSubscription(subscriptionPlan.getId());

        assertThat(retrievedSubscription.getId()).isNotNull();
        assertThat(retrievedSubscription.getPlanPrices()).hasSize(1);
        assertThat(retrievedSubscription.getCountryCode()).hasSize(2);

        assertThat(subscriptionService.getSubscriptionPlans(Locale.TAIWAN.getCountry())).isNotEmpty();
    }
}