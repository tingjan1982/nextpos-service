package io.nextpos.subscription.data;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface ClientSubscriptionRepository extends MongoRepository<ClientSubscription, String> {

    Optional<ClientSubscription> findFirstByClientIdAndSubscriptionPlanSnapshot_IdAndPlanPeriodOrderByCreatedDateDesc(String clientId, String subscriptionPlanId, SubscriptionPlan.PlanPeriod planPeriod);

    ClientSubscription findByClientIdAndCurrentIsTrue(String clientId);

    List<ClientSubscription> findAllByCurrentIsTrueOrStatus(ClientSubscription.SubscriptionStatus subscriptionStatus);

    /**
     * Query annotation is used here to query by date range inclusive, which is otherwise not
     * supported by Spring Data's *Between behavior.
     */
    @Query(value = "{$and: [{ 'status': ?0 }, { 'planEndDate': { $gte: ?1, $lte: ?2 } }]}")
    List<ClientSubscription> findAllByStatusAndPlanEndDateBetween(ClientSubscription.SubscriptionStatus subscriptionStatus, Date from, Date to);

    List<ClientSubscription> findAllByStatus(ClientSubscription.SubscriptionStatus subscriptionStatus);
}
