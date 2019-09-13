package io.nextpos.ordermanagement.service;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.ordermanagement.data.OrderStateChange;
import io.nextpos.ordermanagement.event.LineItemStateChangeEvent;
import io.nextpos.ordermanagement.web.model.UpdateOrderLineItemRequest;

import java.util.List;
import java.util.Optional;

public interface OrderService {

    Order createOrder(Order order);

    Order saveOrder(Order order);

    Order getOrder(String id);

    /**
     * Inflight orders are orders that are still not closed in the current active shift.
     *
     * @param clientId
     * @return
     */
    List<Order> getInflightOrders(String clientId);

    void deleteOrder(Order order);

    OrderStateChange transitionOrderState(Order order, Order.OrderAction orderAction, final Optional<LineItemStateChangeEvent> lineItemStateChangeEvent);

    Order updateOrderLineItem(String id, String lineItemId, UpdateOrderLineItemRequest updateOrderLineItemRequest);

    Order addOrderLineItem(Order order, OrderLineItem orderLineItem);

    List<OrderLineItem> deliverLineItems(String orderId, List<String> lineItemIds);

    Order copyOrder(String id);
}
