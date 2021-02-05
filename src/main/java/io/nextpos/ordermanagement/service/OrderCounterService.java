package io.nextpos.ordermanagement.service;

import io.nextpos.ordermanagement.data.OrderIdCounter;

import java.util.Map;

public interface OrderCounterService {

    Map<String, OrderIdCounter.OrderCounterSummary> getOrderCounterSummaries();
}
