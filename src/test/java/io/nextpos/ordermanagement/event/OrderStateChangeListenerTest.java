package io.nextpos.ordermanagement.event;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderStateChange;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.shared.exception.GeneralApplicationException;
import org.assertj.core.api.AssertionsForClassTypes;
import org.assertj.core.data.Index;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@Transactional
class OrderStateChangeListenerTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private OrderService orderService;

    @Test
    void orderStateChange_HappyPath() throws Exception {

        final Order order = new Order("client-id", BigDecimal.ZERO);
        orderService.createOrder(order);

        final CompletableFuture<OrderStateChange> future = new CompletableFuture<>();
        eventPublisher.publishEvent(new OrderStateChangeEvent(this, order, Order.OrderAction.SUBMIT, new CompletableFuture<>()));
        eventPublisher.publishEvent(new OrderStateChangeEvent(this, order, Order.OrderAction.DELIVER, new CompletableFuture<>()));
        eventPublisher.publishEvent(new OrderStateChangeEvent(this, order, Order.OrderAction.SETTLE, future));

        final OrderStateChange orderStateChange = future.get();

        assertThat(orderStateChange.getOrderId()).isEqualTo(order.getId());
        assertThat(orderStateChange.getStateChanges()).hasSize(3);
        assertThat(orderStateChange.getStateChanges()).satisfies(entry -> assertThat(entry.getToState()).isEqualTo(Order.OrderState.IN_PROCESS), Index.atIndex(0));
        assertThat(orderStateChange.getStateChanges()).satisfies(entry -> assertThat(entry.getToState()).isEqualTo(Order.OrderState.DELIVERED), Index.atIndex(1));
        assertThat(orderStateChange.getStateChanges()).satisfies(entry -> assertThat(entry.getToState()).isEqualTo(Order.OrderState.SETTLED), Index.atIndex(2));
    }

    @Test
    void orderStateChange_Delete() throws Exception {

        final Order order = new Order("client-id", BigDecimal.ZERO);
        orderService.createOrder(order);

        final CompletableFuture<OrderStateChange> future = new CompletableFuture<>();
        eventPublisher.publishEvent(new OrderStateChangeEvent(this, order, Order.OrderAction.DELETE, future));

        final OrderStateChange orderStateChange = future.get();

        assertThat(orderStateChange.getOrderId()).isEqualTo(order.getId());
        assertThat(orderStateChange.getStateChanges()).hasSize(1);
        assertThat(orderStateChange.getStateChanges()).satisfies(entry -> assertThat(entry.getToState()).isEqualTo(Order.OrderState.DELETED), Index.atIndex(0));
    }

    @Test
    void orderStateChange_Cancel() throws Exception {

        final Order order = new Order("client-id", BigDecimal.ZERO);
        orderService.createOrder(order);

        final CompletableFuture<OrderStateChange> future = new CompletableFuture<>();
        eventPublisher.publishEvent(new OrderStateChangeEvent(this, order, Order.OrderAction.SUBMIT, new CompletableFuture<>()));
        eventPublisher.publishEvent(new OrderStateChangeEvent(this, order, Order.OrderAction.CANCEL, future));

        final OrderStateChange orderStateChange = future.get();

        assertThat(orderStateChange.getOrderId()).isEqualTo(order.getId());
        assertThat(orderStateChange.getStateChanges()).hasSize(2);
        assertThat(orderStateChange.getStateChanges()).satisfies(entry -> assertThat(entry.getToState()).isEqualTo(Order.OrderState.IN_PROCESS), Index.atIndex(0));
        assertThat(orderStateChange.getStateChanges()).satisfies(entry -> assertThat(entry.getToState()).isEqualTo(Order.OrderState.CANCELLED), Index.atIndex(1));
    }

    @Test
    void orderStateChange_Refund() throws Exception {

        final Order order = new Order("client-id", BigDecimal.ZERO);
        orderService.createOrder(order);

        final CompletableFuture<OrderStateChange> future = new CompletableFuture<>();
        eventPublisher.publishEvent(new OrderStateChangeEvent(this, order, Order.OrderAction.SUBMIT, new CompletableFuture<>()));
        eventPublisher.publishEvent(new OrderStateChangeEvent(this, order, Order.OrderAction.DELIVER, new CompletableFuture<>()));
        eventPublisher.publishEvent(new OrderStateChangeEvent(this, order, Order.OrderAction.SETTLE, new CompletableFuture<>()));
        eventPublisher.publishEvent(new OrderStateChangeEvent(this, order, Order.OrderAction.REFUND, future));

        final OrderStateChange orderStateChange = future.get();

        assertThat(orderStateChange.getOrderId()).isEqualTo(order.getId());
        assertThat(orderStateChange.getStateChanges()).hasSize(4);
        assertThat(orderStateChange.getStateChanges()).satisfies(entry -> assertThat(entry.getToState()).isEqualTo(Order.OrderState.REFUNDED), Index.atIndex(3));
    }

    /**
     * https://www.baeldung.com/assertj-exception-assertion
     */
    @Test
    void orderStateChange_InvalidTransition() throws Exception {

        final Order order = new Order("client-id", BigDecimal.ZERO);
        orderService.createOrder(order);

        final CompletableFuture<OrderStateChange> future = new CompletableFuture<>();
        eventPublisher.publishEvent(new OrderStateChangeEvent(this, order, Order.OrderAction.SETTLE, future));

        AssertionsForClassTypes.assertThatThrownBy(future::get).isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(GeneralApplicationException.class);
    }
}