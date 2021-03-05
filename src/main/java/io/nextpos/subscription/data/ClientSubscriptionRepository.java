package io.nextpos.subscription.data;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface ClientSubscriptionRepository extends MongoRepository<ClientSubscription, String> {

    Optional<ClientSubscription> findFirstByClientIdAndSubscriptionPlanSnapshot_IdAndPlanPeriodOrderByCreatedDateDesc(String clientId, String subscriptionPlanId, SubscriptionPlan.PlanPeriod planPeriod);

    ClientSubscription findByClientIdAndCurrentIsTrue(String clientId);

    List<ClientSubscription> findAllByCurrentIsTrueOrStatus(ClientSubscription.SubscriptionStatus subscriptionStatus);

    List<ClientSubscription> findAllByStatusAndPlanEndDateBetween(ClientSubscription.SubscriptionStatus subscriptionStatus, Date from, Date to);
}
