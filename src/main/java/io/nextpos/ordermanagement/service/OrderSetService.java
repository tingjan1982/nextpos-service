package io.nextpos.ordermanagement.service;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderSet;

import java.util.List;

public interface OrderSetService {

    OrderSet createOrderSet(String clientId, List<String> orders);

    OrderSet getOrderSet(String id);

    OrderSet getOrderSetByOrderId(String orderId);

    List<OrderSet> getInFlightOrderSets(String clientId);

    Order mergeOrderSet(OrderSet orderSet, String orderIdToMerge);

    OrderSet completeOrderSet(OrderSet orderSet);

    void deleteOrderSet(OrderSet orderSet);
}
