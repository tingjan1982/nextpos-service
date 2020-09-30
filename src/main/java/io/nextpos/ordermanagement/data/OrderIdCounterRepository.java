package io.nextpos.ordermanagement.data;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrderIdCounterRepository extends MongoRepository<OrderIdCounter, String> {
}
