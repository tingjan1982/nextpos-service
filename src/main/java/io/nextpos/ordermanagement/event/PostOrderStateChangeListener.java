package io.nextpos.ordermanagement.event;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderSet;
import io.nextpos.ordermanagement.data.OrderStateChangeBean;
import io.nextpos.ordermanagement.service.OrderSetService;
import io.nextpos.workingarea.data.PrinterInstructions;
import io.nextpos.workingarea.service.PrinterInstructionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class PostOrderStateChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostOrderStateChangeListener.class);

    private final PrinterInstructionService printerInstructionService;

    private final OrderSetService orderSetService;

    @Autowired
    public PostOrderStateChangeListener(final PrinterInstructionService printerInstructionService, OrderSetService orderSetService) {
        this.printerInstructionService = printerInstructionService;
        this.orderSetService = orderSetService;
    }

    @EventListener
    public void postOrderStateChange(PostStateChangeEvent postStateChangeEvent) {

        final Order order = postStateChangeEvent.getOrder();

        LOGGER.info("Post order state change {} for order[{}]", order.getState(), order.getId());

        final OrderStateChangeBean orderStateChangeBean = postStateChangeEvent.getOrderStateChangeBean();

        if (order.getState() == Order.OrderState.IN_PROCESS) {
            LOGGER.info("Creating printer instructions for in process order: {}", order.getId());

            final PrinterInstructions printInstructions = printerInstructionService.createOrderToWorkingArea(order);
            orderStateChangeBean.setPrinterInstructions(printInstructions);
        }

        if (order.getState() == Order.OrderState.COMPLETED) {
            if (order.isOrderSetOrder()) {
                final OrderSet orderSet = orderSetService.getOrderSetByOrderId(order.getId());

                LOGGER.info("Completing the order set {}", orderSet);

                orderSetService.completeOrderSet(orderSet);
            }
        }

        postStateChangeEvent.getFuture().complete(orderStateChangeBean);
    }
}
