package io.nextpos.ordermanagement.service;

import io.nextpos.client.data.Client;
import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.ordermanagement.data.OrderStateChange;
import io.nextpos.ordermanagement.data.OrderStateChangeBean;
import io.nextpos.ordermanagement.event.LineItemStateChangeEvent;
import io.nextpos.ordermanagement.service.bean.UpdateLineItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface OrderService {

    Order createOrder(Order order);

    Order saveOrder(Order order);

    Order getOrder(String id);

    List<Order> getOrders(Client client, ZonedDateRange zonedDateRange);

    List<Order> getOrders(Client client, ZonedDateRange zonedDateRange, String table);

    /**
     * todo: more unit tests
     * Inflight orders are orders that are still not closed in the current active shift.
     *
     * @param clientId
     * @return
     */
    List<Order> getInflightOrders(String clientId);

    List<Order> getOrdersByState(String clientId, Order.OrderState orderState);

    void deleteOrder(Order order);

    void deleteOrderByOrderId(String orderId);

    OrderStateChange transitionOrderState(Order order, Order.OrderAction orderAction, final Optional<LineItemStateChangeEvent> lineItemStateChangeEvent);

    Order updateOrderLineItem(Order order, UpdateLineItem updateLineItem);

    Order updateOrderLineItemPrice(Order order, String lineItemId, BigDecimal overridePrice);

    Order deleteOrderLineItem(Order order, String lineItemId);

    Order addOrderLineItem(Client client, String orderId, OrderLineItem orderLineItem);

    OrderStateChangeBean performOrderAction(String id, Order.OrderAction orderAction);

    Optional<OrderStateChange> getOrderStateChangeByOrderId(String orderId);

    List<OrderLineItem> prepareLineItems(String orderId, List<String> lineItemIds);

    List<OrderLineItem> deliverLineItems(String orderId, List<String> lineItemIds);

    Order copyOrder(String id);

    String generateSerialId(String clientId);
}
