package io.nextpos.ordermanagement.event;

import io.nextpos.ordermanagement.data.*;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.exception.GeneralApplicationException;
import org.assertj.core.data.Index;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;

import javax.transaction.Transactional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class OrderStateChangeListenerTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderSettings orderSettings;

    private Order order;

    @BeforeEach
    void prepare() {
        order = new Order("client-id", orderSettings);
        order.addOrderLineItem(DummyObjects.productSnapshot(), 1);
        orderService.createOrder(order);
    }

    @Test
    void orderStateChange_HappyPath() throws Exception {

        final CompletableFuture<OrderStateChangeBean> future = new CompletableFuture<>();
        eventPublisher.publishEvent(new OrderStateChangeEvent(this, order, Order.OrderAction.SUBMIT, new CompletableFuture<>()));
        eventPublisher.publishEvent(new OrderStateChangeEvent(this, order, Order.OrderAction.DELIVER, future));

        future.get();

        assertThat(order.getState()).isEqualTo(Order.OrderState.DELIVERED);
        assertThat(order.getOrderLineItems()).satisfies(li -> assertThat(li.getState()).isEqualTo(OrderLineItem.LineItemState.DELIVERED), Index.atIndex(0));

        order.addOrderLineItem(DummyObjects.productSnapshot(), 2);
        orderService.saveOrder(order);

        final CompletableFuture<OrderStateChangeBean> resubmittedFuture = new CompletableFuture<>();
        eventPublisher.publishEvent(new OrderStateChangeEvent(this, order, Order.OrderAction.SUBMIT, resubmittedFuture));

        assertThat(order.getState()).isEqualTo(Order.OrderState.IN_PROCESS);
        assertThat(order.getOrderLineItems()).satisfies(li -> assertThat(li.getState()).isEqualTo(OrderLineItem.LineItemState.DELIVERED), Index.atIndex(0));
        assertThat(order.getOrderLineItems()).satisfies(li -> assertThat(li.getState()).isEqualTo(OrderLineItem.LineItemState.IN_PROCESS), Index.atIndex(1));

        eventPublisher.publishEvent(new OrderStateChangeEvent(this, order, Order.OrderAction.DELIVER, new CompletableFuture<>()));

        final CompletableFuture<OrderStateChangeBean> settledFuture = new CompletableFuture<>();

        eventPublisher.publishEvent(new OrderStateChangeEvent(this, order, Order.OrderAction.SETTLE, settledFuture));

        final OrderStateChange orderStateChange = settledFuture.get().getOrderStateChange();

        assertThat(orderStateChange.getOrderId()).isEqualTo(order.getId());
        assertThat(orderStateChange.getStateChanges()).hasSize(5);
        assertThat(orderStateChange.getStateChanges()).satisfies(entry -> assertThat(entry.getToState()).isEqualTo(Order.OrderState.IN_PROCESS), Index.atIndex(0));
        assertThat(orderStateChange.getStateChanges()).satisfies(entry -> assertThat(entry.getToState()).isEqualTo(Order.OrderState.DELIVERED), Index.atIndex(1));
        assertThat(orderStateChange.getStateChanges()).satisfies(entry -> assertThat(entry.getToState()).isEqualTo(Order.OrderState.IN_PROCESS), Index.atIndex(2));
        assertThat(orderStateChange.getStateChanges()).satisfies(entry -> assertThat(entry.getToState()).isEqualTo(Order.OrderState.DELIVERED), Index.atIndex(3));
        assertThat(orderStateChange.getStateChanges()).satisfies(entry -> assertThat(entry.getToState()).isEqualTo(Order.OrderState.SETTLED), Index.atIndex(4));
    }

    @Test
    void orderStateChange_Cancel() throws Exception {

        final CompletableFuture<OrderStateChangeBean> future = new CompletableFuture<>();
        eventPublisher.publishEvent(new OrderStateChangeEvent(this, order, Order.OrderAction.SUBMIT, new CompletableFuture<>()));
        eventPublisher.publishEvent(new OrderStateChangeEvent(this, order, Order.OrderAction.CANCEL, future));

        final OrderStateChange orderStateChange = future.get().getOrderStateChange();

        assertThat(orderStateChange.getOrderId()).isEqualTo(order.getId());
        assertThat(orderStateChange.getStateChanges()).hasSize(2);
        assertThat(orderStateChange.getStateChanges()).satisfies(entry -> assertThat(entry.getToState()).isEqualTo(Order.OrderState.IN_PROCESS), Index.atIndex(0));
        assertThat(orderStateChange.getStateChanges()).satisfies(entry -> assertThat(entry.getToState()).isEqualTo(Order.OrderState.CANCELLED), Index.atIndex(1));
    }

    @Test
    void orderStateChange_Refund() throws Exception {

        final CompletableFuture<OrderStateChangeBean> future = new CompletableFuture<>();
        eventPublisher.publishEvent(new OrderStateChangeEvent(this, order, Order.OrderAction.SUBMIT, new CompletableFuture<>()));
        eventPublisher.publishEvent(new OrderStateChangeEvent(this, order, Order.OrderAction.DELIVER, new CompletableFuture<>()));
        eventPublisher.publishEvent(new OrderStateChangeEvent(this, order, Order.OrderAction.SETTLE, new CompletableFuture<>()));
        eventPublisher.publishEvent(new OrderStateChangeEvent(this, order, Order.OrderAction.REFUND, future));

        final OrderStateChange orderStateChange = future.get().getOrderStateChange();

        assertThat(orderStateChange.getOrderId()).isEqualTo(order.getId());
        assertThat(orderStateChange.getStateChanges()).hasSize(4);
        assertThat(orderStateChange.getStateChanges()).satisfies(entry -> assertThat(entry.getToState()).isEqualTo(Order.OrderState.REFUNDED), Index.atIndex(3));
    }

    @Test
    void orderStateChange_Close() throws Exception {

        final CompletableFuture<OrderStateChangeBean> future = new CompletableFuture<>();
        eventPublisher.publishEvent(new OrderStateChangeEvent(this, order, Order.OrderAction.SUBMIT, new CompletableFuture<>()));
        eventPublisher.publishEvent(new OrderStateChangeEvent(this, order, Order.OrderAction.DELIVER, new CompletableFuture<>()));
        eventPublisher.publishEvent(new OrderStateChangeEvent(this, order, Order.OrderAction.SETTLE, new CompletableFuture<>()));
        eventPublisher.publishEvent(new OrderStateChangeEvent(this, order, Order.OrderAction.COMPLETE, future));

        final OrderStateChange orderStateChange = future.get().getOrderStateChange();

        assertThat(orderStateChange.getOrderId()).isEqualTo(order.getId());
        assertThat(orderStateChange.getStateChanges()).hasSize(4);
        assertThat(orderStateChange.getStateChanges()).satisfies(entry -> assertThat(entry.getToState()).isEqualTo(Order.OrderState.COMPLETED), Index.atIndex(3));
    }

    /**
     * https://www.baeldung.com/assertj-exception-assertion
     */
    @Test
    void orderStateChange_InvalidTransition() {

        final CompletableFuture<OrderStateChangeBean> future = new CompletableFuture<>();
        eventPublisher.publishEvent(new OrderStateChangeEvent(this, order, Order.OrderAction.SETTLE, future));

        assertThatThrownBy(future::get).isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(GeneralApplicationException.class);
    }
}