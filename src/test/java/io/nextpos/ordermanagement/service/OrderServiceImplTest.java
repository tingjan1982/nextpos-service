package io.nextpos.ordermanagement.service;

import io.nextpos.ordermanagement.data.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@Transactional
class OrderServiceImplTest {

    @Autowired
    private OrderService orderService;


    @Test
    void createAndGetOrder() {

        final Order order = Order.builder().build();

        final Order createdOrder = orderService.createOrder(order);

        assertThat(createdOrder.getId()).isNotNull();
        assertThat(createdOrder.getState()).isEqualTo(Order.OrderState.NEW);

        final Order existingOrder = orderService.getOrder(createdOrder.getId());

        assertThat(existingOrder).isEqualTo(createdOrder);
    }

    @Test
    void deleteOrder() {

        final Order createdOrder = orderService.createOrder(Order.builder().build());

        orderService.deleteOrder(createdOrder);

        assertThat(orderService.orderExists(createdOrder.getId())).isFalse();
    }
}