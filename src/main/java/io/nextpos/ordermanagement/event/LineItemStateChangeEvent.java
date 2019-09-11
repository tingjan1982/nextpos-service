package io.nextpos.ordermanagement.event;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;
import org.springframework.context.ApplicationEvent;

import java.util.List;

public class LineItemStateChangeEvent extends ApplicationEvent {

    private final Order order;

    private final Order.OrderAction orderAction;

    private final List<OrderLineItem> lineItems;

    public LineItemStateChangeEvent(final Object source, final Order order, final Order.OrderAction orderAction, final List<OrderLineItem> lineItems) {
        super(source);
        this.order = order;
        this.orderAction = orderAction;
        this.lineItems = lineItems;
    }

    public Order getOrder() {
        return order;
    }

    Order.OrderAction getOrderAction() {
        return orderAction;
    }

    List<OrderLineItem> getLineItems() {
        return lineItems;
    }
}
