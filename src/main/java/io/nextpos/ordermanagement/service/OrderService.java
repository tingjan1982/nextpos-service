package io.nextpos.ordermanagement.service;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.ordermanagement.data.OrderStateChange;
import io.nextpos.ordermanagement.web.model.UpdateOrderLineItemRequest;

public interface OrderService {

    Order createOrder(Order order);

    Order getOrder(String id);

    void deleteOrder(Order order);

    OrderStateChange transitionOrderState(Order order, Order.OrderState orderState);

    Order updateOrderLineItem(String id, String lineItemId, UpdateOrderLineItemRequest updateOrderLineItemRequest);

    Order addOrderLineItem(Order order, OrderLineItem orderLineItem);
}
