package io.nextpos.subscription.data;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ClientSubscriptionRepository extends MongoRepository<ClientSubscription, String> {

    ClientSubscription findFirstByClientIdOrderByCreatedDateDesc(String clientId);
}
