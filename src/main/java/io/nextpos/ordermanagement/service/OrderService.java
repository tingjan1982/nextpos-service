package io.nextpos.ordermanagement.service;

import io.nextpos.ordermanagement.data.Order;

public interface OrderService {

    Order createOrder(Order order);

    Order getOrder(String id);

    boolean orderExists(String id);

    void deleteOrder(Order order);
}
