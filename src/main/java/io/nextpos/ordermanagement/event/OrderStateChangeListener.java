package io.nextpos.ordermanagement.event;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderStateChange;
import io.nextpos.ordermanagement.data.OrderStateChangeBean;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.shared.exception.GeneralApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * How to use event:
 * https://www.baeldung.com/spring-events
 *
 * How to use CompletableFuture:
 * https://www.baeldung.com/java-completablefuture
 */
@Component
public class OrderStateChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderStateChangeListener.class);

    private final OrderService orderService;

    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public OrderStateChangeListener(final OrderService orderService, final ApplicationEventPublisher eventPublisher) {
        this.orderService = orderService;
        this.eventPublisher = eventPublisher;
    }

    @EventListener
    public void orderStateChange(OrderStateChangeEvent event) {

        final Order order = event.getOrder();
        final Order.OrderAction orderAction = event.getOrderAction();
        final Order.OrderState orderState = order.getState();

        LOGGER.info("Order state: {}, Order Action: {}", orderState, orderAction);

        if (orderAction.getValidFromState().contains(orderState)) {
            final Order.OrderState transitionState = orderAction.getValidNextState();
            LOGGER.info("Order in valid state, proceed with order transition: {}", transitionState);

            final OrderStateChange orderStateChange = orderService.transitionOrderState(order, transitionState);

            LOGGER.info("Updated order state change: {}", orderStateChange);

            final OrderStateChangeBean orderStateChangeBean = new OrderStateChangeBean(orderStateChange);

            // dispatch a post state change event.
            eventPublisher.publishEvent(new PostStateChangeEvent(this, order, orderStateChangeBean, event.getFuture()));
        }

        final String errorMsg = String.format("Unable to process order action [%s] from the order state [%s], [orderId=%s]", orderAction, orderState, order.getId());
        event.getFuture().completeExceptionally(new GeneralApplicationException(errorMsg));
    }
}
