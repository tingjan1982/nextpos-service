package io.nextpos.ordermanagement.event;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderStateChangeBean;
import org.springframework.context.ApplicationEvent;

import java.util.concurrent.CompletableFuture;

public class OrderStateChangeEvent extends ApplicationEvent {

    private final Order order;

    private final Order.OrderAction orderAction;

    private final CompletableFuture<OrderStateChangeBean> future;

    public OrderStateChangeEvent(final Object source, final Order order, final Order.OrderAction orderAction, final CompletableFuture<OrderStateChangeBean> future) {
        super(source);

        this.order = order;
        this.orderAction = orderAction;
        this.future = future;
    }

    public Order getOrder() {
        return order;
    }

    Order.OrderAction getOrderAction() {
        return orderAction;
    }

    CompletableFuture<OrderStateChangeBean> getFuture() {
        return future;
    }
}
