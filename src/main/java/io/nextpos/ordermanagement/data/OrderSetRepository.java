package io.nextpos.ordermanagement.data;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface OrderSetRepository extends MongoRepository<OrderSet, String> {

    OrderSet findByMainOrderId(String orderId);

    List<OrderSet> findAllByClientIdAndStatusIsNot(String clientId, OrderSet.OrderSetStatus orderSetStatus);
}
