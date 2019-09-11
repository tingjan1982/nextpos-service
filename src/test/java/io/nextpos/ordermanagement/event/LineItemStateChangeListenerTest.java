package io.nextpos.ordermanagement.event;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.settings.data.CountrySettings;
import io.nextpos.shared.DummyObjects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;

import javax.transaction.Transactional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class LineItemStateChangeListenerTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CountrySettings countrySettings;

    @Test
    void lineItemStateChange() {

        final Order order = new Order("client", countrySettings.getTaxRate(), countrySettings.getCurrency());
        order.addOrderLineItem(DummyObjects.productSnapshot(), 1);
        order.addOrderLineItem(DummyObjects.productSnapshot(), 2);
        orderService.saveOrder(order);

        eventPublisher.publishEvent(new OrderStateChangeEvent(this, order, Order.OrderAction.SUBMIT, new CompletableFuture<>()));

        eventPublisher.publishEvent(new LineItemStateChangeEvent(this, order, Order.OrderAction.PARTIAL_DELIVER, order.getOrderLineItems()));

        assertThat(order.getState()).isEqualTo(Order.OrderState.DELIVERED);
    }
}