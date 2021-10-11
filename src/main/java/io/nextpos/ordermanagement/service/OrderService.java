package io.nextpos.ordermanagement.service;

import io.nextpos.client.data.Client;
import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.ordermanagement.data.*;
import io.nextpos.ordermanagement.event.LineItemStateChangeEvent;
import io.nextpos.ordermanagement.service.bean.LineItemOrdering;
import io.nextpos.ordermanagement.service.bean.UpdateLineItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface OrderService {

    Order createOrder(Order order);

    Optional<Order> getOrderByTableId(Client client, String tableId);

    Order saveOrder(Order order);

    Order getOrder(String id);

    List<Order> getOrders(Client client, ZonedDateRange zonedDateRange);

    List<Order> getOrders(Client client, ZonedDateRange zonedDateRange, OrderCriteria orderCriteria);

    /**
     * todo: more unit tests
     * Inflight orders are orders that are still not closed in the current active shift.
     *
     * @param clientId
     * @return
     */
    List<Order> getInflightOrders(String clientId);

    List<Order> getOrdersByStates(String clientId, List<Order.OrderState> orderStates);

    List<Order> getInStoreInFlightOrders(String clientId);

    InProcessOrderLineItems getInProcessOrderLineItems(String clientId);

    InProcessOrders getInProcessOrders(String clientId);

    void markAllLineItemsAsPrepared(String clientId);

    Order moveOrder(String sourceOrderId, String targetOrderId);

    void deleteOrder(String orderId);

    OrderStateChange transitionOrderState(Order order, Order.OrderAction orderAction, final Optional<LineItemStateChangeEvent> lineItemStateChangeEvent);

    void markOrderAsDeleted(String orderId, boolean shiftAudit);

    Order updateOrderLineItem(Order order, UpdateLineItem updateLineItem);

    Order updateOrderLineItemPrice(Order order, String lineItemId, BigDecimal overridePrice);

    Order deleteOrderLineItem(Order order, String lineItemId);

    Order addOrderLineItem(Client client, Order order, OrderLineItem orderLineItem);

    OrderStateChangeBean performOrderAction(String id, Order.OrderAction orderAction);

    OrderStateChangeBean performOrderAction(Order order, Order.OrderAction orderAction);

    Optional<OrderStateChange> getOrderStateChangeByOrderId(String orderId);

    Order prepareLineItems(String orderId, List<String> lineItemIds);

    Order deliverLineItems(String orderId, List<String> lineItemIds);

    Order moveLineItems(Order fromOrder, Order toOrder, List<String> lineItemIds);

    Order copyOrder(String id);

    String generateSerialId(String clientId);

    void orderLineItems(List<LineItemOrdering> lineItemOrderings);

    void reorder(List<String> orderIds);
}
