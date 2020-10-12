package io.nextpos.subscription.service;

import io.nextpos.subscription.data.SubscriptionPlan;

import java.util.List;

public interface SubscriptionService {

    SubscriptionPlan saveSubscriptionPlan(SubscriptionPlan subscriptionPlan);

    SubscriptionPlan getSubscription(String id);

    List<SubscriptionPlan> getSubscriptionPlans(String countryCode);

    void deleteSubscriptionPlan(SubscriptionPlan subscriptionPlan);
}
