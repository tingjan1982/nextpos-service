package io.nextpos.subscription.service;

import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import io.nextpos.subscription.data.SubscriptionPlan;
import io.nextpos.subscription.data.SubscriptionPlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ChainedTransaction
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;

    @Autowired
    public SubscriptionServiceImpl(SubscriptionPlanRepository subscriptionPlanRepository) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
    }

    @Override
    public SubscriptionPlan saveSubscriptionPlan(SubscriptionPlan subscriptionPlan) {
        return subscriptionPlanRepository.save(subscriptionPlan);
    }

    @Override
    public SubscriptionPlan getSubscription(String id) {
        return subscriptionPlanRepository.findById(id).orElseThrow(() -> {
            throw new ObjectNotFoundException(id, SubscriptionPlan.class);
        });
    }

    @Override
    public List<SubscriptionPlan> getSubscriptionPlans(String countryCode) {
        return subscriptionPlanRepository.findAllByCountryCode(countryCode);
    }

    @Override
    public void deleteSubscriptionPlan(SubscriptionPlan subscriptionPlan) {
        subscriptionPlanRepository.delete(subscriptionPlan);
    }
}
