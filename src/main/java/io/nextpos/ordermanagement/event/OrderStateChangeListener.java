package io.nextpos.ordermanagement.event;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.ordermanagement.data.OrderStateChange;
import io.nextpos.ordermanagement.data.OrderStateChangeBean;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.shared.exception.BusinessLogicException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * How to use event:
 * https://www.baeldung.com/spring-events
 * <p>
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

        LOGGER.info("Order[{}] in the state[{}] received action[{}]", order.getId(), orderState, orderAction);

        if (orderAction.getValidFromState().contains(orderState)) {
            if (canOrderActionContinue(orderAction, order)) {
                final Optional<LineItemStateChangeEvent> lineItemStateChangeEvent = Optional.of(
                        new LineItemStateChangeEvent(this, order, orderAction, order.getOrderLineItems())
                );

                final OrderStateChange orderStateChange = orderService.transitionOrderState(order, orderAction, lineItemStateChangeEvent);
                final OrderStateChangeBean orderStateChangeBean = new OrderStateChangeBean(orderStateChange);

                // dispatch a post state change event.
                eventPublisher.publishEvent(new PostStateChangeEvent(this, order, orderStateChangeBean, event.getFuture()));
            } else {
                orderService.getOrderStateChangeByOrderId(order.getId()).ifPresent(sc -> event.getFuture().complete(new OrderStateChangeBean(sc)));
            }
        }

        final String errorMsg = String.format("Unable to process order action [%s] from the order state [%s], [orderId=%s]", orderAction, orderState, order.getId());
        LOGGER.error("{}", errorMsg);

        event.getFuture().completeExceptionally(new BusinessLogicException("message.unableToChangeState", errorMsg));
    }

    private boolean canOrderActionContinue(Order.OrderAction orderAction, Order order) {

        if (orderAction == Order.OrderAction.SUBMIT) {
            final boolean allLineItemsDelivered = order.getOrderLineItems().stream().allMatch(li -> li.getState() == OrderLineItem.LineItemState.DELIVERED);

            if (allLineItemsDelivered) {
                LOGGER.warn("All line items are delivered therefore submit action will be skipped at this time.");
                return false;
            }
        }

        return true;
    }
}
