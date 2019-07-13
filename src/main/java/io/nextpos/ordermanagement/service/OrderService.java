package io.nextpos.ordermanagement.service;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderStateChange;

public interface OrderService {

    Order createOrder(Order order);

    Order getOrder(String id);

    boolean orderExists(String id);

    OrderStateChange transitionOrderState(Order order, Order.OrderState orderState);
}
