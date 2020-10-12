package io.nextpos.subscription.data;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SubscriptionPlanRepository extends MongoRepository<SubscriptionPlan, String> {

    List<SubscriptionPlan> findAllByCountryCode(String countryCode);
}
