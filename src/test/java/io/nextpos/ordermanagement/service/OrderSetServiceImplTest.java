package io.nextpos.ordermanagement.service;

import io.nextpos.client.data.Client;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderSet;
import io.nextpos.ordermanagement.data.OrderSettings;
import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.exception.ObjectNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class OrderSetServiceImplTest {

    private final OrderSetService orderSetService;

    private final OrderService orderService;

    private final Client client;

    private final OrderSettings orderSettings;

    @Autowired
    OrderSetServiceImplTest(OrderSetService orderSetService, OrderService orderService, Client client, OrderSettings orderSettings) {
        this.orderSetService = orderSetService;
        this.orderService = orderService;
        this.client = client;
        this.orderSettings = orderSettings;
    }

    @Test
    void createOrderSet() {

        Order order1 = createDummyOrder();
        Order order2 = createDummyOrder();
        Order order3 = createDummyOrder();

        final OrderSet orderSet = orderSetService.createOrderSet(client.getId(), Arrays.asList(order1.getId(), order2.getId(), order3.getId()));

        assertThat(orderSet.getId()).isNotNull();
        assertThat(orderSet.getLinkedOrders()).hasSize(3);
        assertThat(orderSet.getMainOrderId()).isEqualTo(order1.getId());

        final Order mergedOrder = orderSetService.mergeOrderSet(orderSet);

        assertThat(mergedOrder.getOrderLineItems()).hasSize(3);
        assertThat(mergedOrder.isOrderSetOrder()).isTrue();

        assertThat(orderService.getOrder(order1.getId()).getId()).isEqualTo(mergedOrder.getId());
        assertThat(orderService.getOrder(order2.getId()).getOrderLineItems()).isEmpty();
        assertThat(orderService.getOrder(order3.getId()).getOrderLineItems()).isEmpty();

        assertThat(orderSetService.getOrderSetByOrderId(mergedOrder.getId())).isNotNull();
        assertThat(orderSetService.getInFlightOrderSets(client.getId())).hasSize(1);
    }

    @Test
    void testDeleteOrderSet() {

        Order order1 = createDummyOrder();
        Order order2 = createDummyOrder();

        final OrderSet orderSet = orderSetService.createOrderSet(client.getId(), Arrays.asList(order1.getId(), order2.getId()));

        orderSetService.deleteOrderSet(orderSet);
        assertThatThrownBy(() -> orderSetService.getOrderSet(orderSet.getId())).isInstanceOf(ObjectNotFoundException.class);
    }

    private Order createDummyOrder() {
        final Order order = new Order(client.getId(), orderSettings);
        order.addOrderLineItem(DummyObjects.productSnapshot(), 1);

        return orderService.saveOrder(order);
    }
}