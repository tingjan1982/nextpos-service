package io.nextpos.ordermanagement.event;

import io.nextpos.client.data.Client;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.ordermanagement.data.OrderSettings;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ChainedTransaction
class LineItemStateChangeListenerTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderSettings orderSettings;

    @Autowired
    private Client client;

    @Test
    void lineItemStateChange() {

        final Order order = new Order(client.getId(), orderSettings);
        order.addOrderLineItem(DummyObjects.productSnapshot(), 1);
        order.addOrderLineItem(DummyObjects.productSnapshot(), 2);
        orderService.createOrder(order);

        eventPublisher.publishEvent(new OrderStateChangeEvent(this, order, Order.OrderAction.SUBMIT, new CompletableFuture<>()));

        eventPublisher.publishEvent(new LineItemStateChangeEvent(this, order, Order.OrderAction.PARTIAL_DELIVER, order.getOrderLineItems()));

        assertThat(order.getOrderLineItems()).allMatch(li -> li.getState() == OrderLineItem.LineItemState.DELIVERED);
        assertThat(order.getState()).isEqualTo(Order.OrderState.DELIVERED);
    }
}