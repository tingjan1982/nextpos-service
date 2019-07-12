package io.nextpos.ordermanagement.service;

import io.nextpos.ordermanagement.data.Order;

import java.util.Optional;

public interface OrderService {

    Order createOrder(Order order);

    Optional<Order> getOrder(String id);

    boolean orderExists(String id);
}
