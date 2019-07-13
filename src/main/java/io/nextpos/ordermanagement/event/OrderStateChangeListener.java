package io.nextpos.ordermanagement.event;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderStateChange;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.shared.exception.GeneralApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public OrderStateChangeListener(final OrderService orderService) {
        this.orderService = orderService;
    }

    @EventListener
    public void orderStateChange(OrderStateChangeEvent event) {

        final Order order = event.getOrder();
        final Order.OrderAction orderAction = event.getOrderAction();
        final Order.OrderState orderState = order.getState();

        LOGGER.info("Order state: {}, Order Action: {}", orderState, orderAction);

        if (orderAction.getValidStartState() == orderState) {
            final Order.OrderState transitionState = orderAction.getValidNextState();
            LOGGER.info("Order in valid state, proceed with order transition: {}", transitionState);

            final OrderStateChange orderStateChange = orderService.transitionOrderState(order, transitionState);

            LOGGER.info("Updated order state change: {}", orderStateChange);

            event.getFuture().complete(orderStateChange);
        }

        event.getFuture().completeExceptionally(new GeneralApplicationException("Unable to process state transition on order id: " + order.getId()));
    }
}
