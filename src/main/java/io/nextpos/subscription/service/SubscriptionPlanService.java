package io.nextpos.subscription.service;

import io.nextpos.subscription.data.SubscriptionPaymentInstruction;
import io.nextpos.subscription.data.SubscriptionPlan;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPlanService {

    SubscriptionPlan saveSubscriptionPlan(SubscriptionPlan subscriptionPlan);

    SubscriptionPlan getSubscription(String id);

    List<SubscriptionPlan> getSubscriptionPlans(String countryCode);

    void deleteSubscriptionPlan(SubscriptionPlan subscriptionPlan);

    SubscriptionPaymentInstruction saveSubscriptionPaymentInstruction(SubscriptionPaymentInstruction instruction);

    Optional<SubscriptionPaymentInstruction> getSubscriptionPaymentInstructionByCountry(String countryCode);

    SubscriptionPaymentInstruction getSubscriptionPaymentInstructionByCountryOrThrows(String countryCode);
}
