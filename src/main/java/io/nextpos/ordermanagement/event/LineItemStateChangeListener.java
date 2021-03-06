package io.nextpos.ordermanagement.event;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.ordermanagement.service.OrderMessagingService;
import io.nextpos.ordermanagement.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class LineItemStateChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LineItemStateChangeListener.class);

    private final OrderService orderService;

    private final OrderMessagingService orderMessagingService;

    @Autowired
    public LineItemStateChangeListener(final OrderService orderService, final OrderMessagingService orderMessagingService) {
        this.orderService = orderService;
        this.orderMessagingService = orderMessagingService;
    }

    /**
     * Handles line item state transition, putting the responsibility of persistence to the event publishing initiator.
     */
    @EventListener
    public void lineItemStateChange(LineItemStateChangeEvent lineItemStateChangeEvent) {

        final Order.OrderAction orderAction = lineItemStateChangeEvent.getOrderAction();
        final Order order = lineItemStateChangeEvent.getOrder();
        LOGGER.info("Handle line item state change for order[{}], action[{}]", order.getId(), orderAction);

        final List<OrderLineItem> lineItems = lineItemStateChangeEvent.getLineItems();

        switch (orderAction) {
            case SUBMIT:
                lineItems.forEach(li -> {
                    final OrderLineItem.LineItemState state = li.getState();

                    switch (state) {
                        case OPEN:
                            li.setState(OrderLineItem.LineItemState.IN_PROCESS);
                            break;
                        case IN_PROCESS:
                            li.setState(OrderLineItem.LineItemState.ALREADY_IN_PROCESS);
                            break;
                    }
                });
                break;
            case PREPARE:
                lineItems.forEach(li -> {
                    if (li.getState().isPreparing()) {
                        li.setState(OrderLineItem.LineItemState.PREPARED);
                    }
                });

                break;
            case DELIVER:

            case POST_DELIVER:
                lineItems.forEach(li -> li.setState(OrderLineItem.LineItemState.DELIVERED));
                break;

            case PARTIAL_DELIVER:
                lineItems.forEach(li -> li.setState(OrderLineItem.LineItemState.DELIVERED));

                final boolean allDelivered = order.getOrderLineItems().stream().allMatch(li -> li.getState() == OrderLineItem.LineItemState.DELIVERED);

                final boolean canChangeOrderState = !order.isClosed();

                if (allDelivered && canChangeOrderState) {
                    LOGGER.info("All order line items are delivered, auto transition order to {}", orderAction);
                    orderService.transitionOrderState(order, orderAction, Optional.empty());
                }

                break;
        }

        orderService.saveOrder(order);
        orderMessagingService.sendOrderLineItems(order.getClientId(), true);
        orderMessagingService.sendOrders(order.getClientId(), true);

        LOGGER.info("Line item state changes are completed: {}", lineItems);
    }
}
