package io.nextpos.ordermanagement.event;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderStateChangeBean;
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

    @Autowired
    public PostOrderStateChangeListener(final PrinterInstructionService printerInstructionService) {
        this.printerInstructionService = printerInstructionService;
    }

    @EventListener
    public void postOrderStateChange(PostStateChangeEvent postStateChangeEvent) {

        LOGGER.info("Post order state change: {}", postStateChangeEvent);

        final Order order = postStateChangeEvent.getOrder();
        final OrderStateChangeBean orderStateChangeBean = postStateChangeEvent.getOrderStateChangeBean();

        if (order.getState() == Order.OrderState.IN_PROCESS) {
            LOGGER.info("Creating printer instructions for in process order: {}", order.getId());

            final PrinterInstructions printInstructions = printerInstructionService.createOrderToWorkingArea(order);
            orderStateChangeBean.setPrinterInstructions(printInstructions);
        }

        postStateChangeEvent.getFuture().complete(orderStateChangeBean);
    }
}
