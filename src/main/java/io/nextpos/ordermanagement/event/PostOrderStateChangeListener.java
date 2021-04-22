package io.nextpos.ordermanagement.event;

import io.nextpos.ordermanagement.boundedcontext.InventoryTransactionContextualService;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderSet;
import io.nextpos.ordermanagement.data.OrderStateChangeBean;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.ordermanagement.service.OrderSetService;
import io.nextpos.workingarea.data.PrinterInstructions;
import io.nextpos.workingarea.service.PrinterInstructionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class PostOrderStateChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostOrderStateChangeListener.class);

    private final PrinterInstructionService printerInstructionService;

    private final InventoryTransactionContextualService inventoryTransactionContextualService;

    private final OrderService orderService;
    
    private final OrderSetService orderSetService;

    @Autowired
    public PostOrderStateChangeListener(final PrinterInstructionService printerInstructionService, InventoryTransactionContextualService inventoryTransactionContextualService, OrderService orderService, OrderSetService orderSetService) {
        this.printerInstructionService = printerInstructionService;
        this.inventoryTransactionContextualService = inventoryTransactionContextualService;
        this.orderService = orderService;
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

        if (order.getState() == Order.OrderState.SETTLED) {
            inventoryTransactionContextualService.createAndProcessInventoryTransaction(order);
        }

        handleSplitOrder(order);
        handleOrderSetStateChange(order);

        postStateChangeEvent.getFuture().complete(orderStateChangeBean);
    }

    private void handleSplitOrder(Order order) {

        if (order.isSplitOrder() && order.getState() == Order.OrderState.COMPLETED) {
            final String sourceOrderId = order.getSourceOrderId();
            final Order sourceOrder = orderService.getOrder(sourceOrderId);

            if (CollectionUtils.isEmpty(sourceOrder.getOrderLineItems())) {
                orderService.deleteOrder(sourceOrderId);
            }
        }
    }

    private void handleOrderSetStateChange(Order order) {

        if (order.isOrderSetOrder()) {
            if (order.getState() == Order.OrderState.SETTLED) {
                final OrderSet orderSet = orderSetService.getOrderSetByOrderId(order.getId());
                LOGGER.info("Settling the order set {}", orderSet);

                orderSetService.settleOrderSet(orderSet);

            } else if (order.getState() == Order.OrderState.COMPLETED) {
                final OrderSet orderSet = orderSetService.getOrderSetByOrderId(order.getId());
                LOGGER.info("Completing the order set {}", orderSet);

                orderSetService.completeOrderSet(orderSet);
            }
        }
    }
}
