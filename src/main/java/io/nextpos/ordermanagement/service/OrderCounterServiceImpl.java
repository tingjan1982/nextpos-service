package io.nextpos.ordermanagement.service;

import io.nextpos.ordermanagement.data.OrderIdCounter;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@ChainedTransaction
public class OrderCounterServiceImpl implements OrderCounterService {

    private final MongoTemplate mongoTemplate;

    @Autowired
    public OrderCounterServiceImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Map<String, OrderIdCounter.OrderCounterSummary> getOrderCounterSummaries() {

        ProjectionOperation projection = Aggregation.project("clientId", "orderIdPrefix", "counter");
        GroupOperation groupedOrderCount = Aggregation.group("clientId")
                .first("clientId").as("clientId")
                .last("orderIdPrefix").as("orderIdPrefix")
                .sum("counter").as("orderCount");

        TypedAggregation<OrderIdCounter> aggregation = Aggregation.newAggregation(OrderIdCounter.class,
                projection,
                groupedOrderCount);
        final AggregationResults<OrderIdCounter.OrderCounterSummary> results = mongoTemplate.aggregate(aggregation, OrderIdCounter.OrderCounterSummary.class);

        return results.getMappedResults().stream()
                .collect(Collectors.toMap(OrderIdCounter.OrderCounterSummary::getClientId, s -> s));
    }
}
