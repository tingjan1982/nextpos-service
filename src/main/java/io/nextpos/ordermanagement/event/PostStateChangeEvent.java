package io.nextpos.ordermanagement.event;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderStateChangeBean;
import org.springframework.context.ApplicationEvent;

import java.util.concurrent.CompletableFuture;

public class PostStateChangeEvent extends ApplicationEvent {

    private final Order order;

    private final OrderStateChangeBean orderStateChangeBean;

    private final CompletableFuture<OrderStateChangeBean> future;

    public PostStateChangeEvent(final Object source, final Order order, final OrderStateChangeBean orderStateChangeBean, final CompletableFuture<OrderStateChangeBean> future) {
        super(source);

        this.order = order;
        this.orderStateChangeBean = orderStateChangeBean;
        this.future = future;
    }

    public Order getOrder() {
        return order;
    }

    public OrderStateChangeBean getOrderStateChangeBean() {
        return orderStateChangeBean;
    }

    public CompletableFuture<OrderStateChangeBean> getFuture() {
        return future;
    }
}
