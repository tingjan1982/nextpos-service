package io.nextpos.subscription.data;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ClientSubscriptionRepository extends MongoRepository<ClientSubscription, String> {

    Optional<ClientSubscription> findByClientIdAndSubscriptionPlanSnapshot_IdAndPlanPeriod(String clientId, String subscriptionPlanId, SubscriptionPlan.PlanPeriod planPeriod);

    ClientSubscription findFirstByClientIdOrderByCreatedDateDesc(String clientId);
}
