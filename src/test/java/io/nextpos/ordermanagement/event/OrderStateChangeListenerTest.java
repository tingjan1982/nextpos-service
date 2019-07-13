package io.nextpos.ordermanagement.event;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderStateChange;
import io.nextpos.ordermanagement.service.OrderService;
import org.assertj.core.data.Index;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

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
    void orderStateChange() throws Exception {

        final Order order = new Order("client-id", BigDecimal.ZERO);
        orderService.createOrder(order);

        final CompletableFuture<OrderStateChange> future = new CompletableFuture<>();
        eventPublisher.publishEvent(new OrderStateChangeEvent(this, order, Order.OrderAction.SUBMIT, future));

        final OrderStateChange orderStateChange = future.get();

        assertThat(orderStateChange.getOrderId()).isEqualTo(order.getId());
        assertThat(orderStateChange.getStateChanges()).hasSize(1);
        assertThat(orderStateChange.getStateChanges()).satisfies(entry -> {
            assertThat(entry.getToState()).isEqualTo(Order.OrderState.IN_PROCESS);
        }, Index.atIndex(0));
    }
}