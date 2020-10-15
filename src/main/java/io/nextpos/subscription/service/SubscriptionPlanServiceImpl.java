package io.nextpos.subscription.service;

import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import io.nextpos.subscription.data.SubscriptionPaymentInstruction;
import io.nextpos.subscription.data.SubscriptionPaymentInstructionRepository;
import io.nextpos.subscription.data.SubscriptionPlan;
import io.nextpos.subscription.data.SubscriptionPlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@ChainedTransaction
public class SubscriptionPlanServiceImpl implements SubscriptionPlanService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;

    private final SubscriptionPaymentInstructionRepository subscriptionPaymentInstructionRepository;

    @Autowired
    public SubscriptionPlanServiceImpl(SubscriptionPlanRepository subscriptionPlanRepository, SubscriptionPaymentInstructionRepository subscriptionPaymentInstructionRepository) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.subscriptionPaymentInstructionRepository = subscriptionPaymentInstructionRepository;
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

    @Override
    public SubscriptionPaymentInstruction saveSubscriptionPaymentInstruction(SubscriptionPaymentInstruction instruction) {
        return subscriptionPaymentInstructionRepository.save(instruction);
    }

    @Override
    public Optional<SubscriptionPaymentInstruction> getSubscriptionPaymentInstructionByCountry(String countryCode) {
        return subscriptionPaymentInstructionRepository.findByCountryCode(countryCode);
    }

    @Override
    public SubscriptionPaymentInstruction getSubscriptionPaymentInstructionByCountryOrThrows(String countryCode) {
        return subscriptionPaymentInstructionRepository.findByCountryCode(countryCode).orElseThrow(() -> {
            throw new ObjectNotFoundException(countryCode, SubscriptionPaymentInstruction.class);
        });
    }
}
