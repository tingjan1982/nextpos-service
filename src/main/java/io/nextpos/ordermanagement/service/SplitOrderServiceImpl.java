package io.nextpos.ordermanagement.service;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.ordermanagement.data.SplitAmountDetails;
import io.nextpos.ordertransaction.service.OrderTransactionService;
import io.nextpos.shared.exception.BusinessLogicException;
import io.nextpos.shared.service.annotation.MongoTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@MongoTransaction
public class SplitOrderServiceImpl implements SplitOrderService {

    private final OrderService orderService;

    private final OrderTransactionService orderTransactionService;

    @Autowired
    public SplitOrderServiceImpl(OrderService orderService, OrderTransactionService orderTransactionService) {
        this.orderService = orderService;
        this.orderTransactionService = orderTransactionService;
    }

    /**
     * Creates and returns target order from source order.
     * <p>
     * omitting demographic data and table info.
     */
    @Override
    public Order newSplitOrder(String sourceOrderId, String sourceLineItemId) {

        final Order sourceOrder = orderService.getOrder(sourceOrderId);

        final Order targetOrder = Order.newOrder(sourceOrder.getClientId(), sourceOrder.getOrderType(), sourceOrder.getOrderSettings());
        targetOrder.setState(sourceOrder.getState());
        targetOrder.markSplitOrder(sourceOrder);

        orderService.createOrder(targetOrder);

        if (sourceLineItemId != null) {
            return updateSourceAndTargetOrder(sourceOrder, targetOrder, sourceLineItemId);
        } else {
            return targetOrder;
        }
    }

    /**
     * Updates source and target order with adjusted line item quantity and return target order.
     */
    @Override
    public Order updateLineItem(String sourceOrderId, String targetOrderId, String sourceLineItemId) {

        final Order sourceOrder = orderService.getOrder(sourceOrderId);
        final Order targetOrder = orderService.getOrder(targetOrderId);

        return updateSourceAndTargetOrder(sourceOrder, targetOrder, sourceLineItemId);
    }

    /**
     * Move all split line items back to source order.
     */
    @Override
    public Order revertSplitOrderLineItems(String splitOrderId, String sourceOrderId) {

        final Order sourceOrder = orderService.getOrder(sourceOrderId);
        final Order splitOrder = orderService.getOrder(splitOrderId);

        for (OrderLineItem li : splitOrder.getOrderLineItems()) {
            sourceOrder.findOrderLineItem(li.getId()).ifPresentOrElse(l1 -> {
                sourceOrder.updateOrderLineItem(l1, l2 -> l2.incrementQuantity(li.getQuantity()));
            }, () -> {
                sourceOrder.getOrderLineItems().add(li);
                sourceOrder.computeTotal();
            });
        }

        orderService.deleteOrder(splitOrderId);

        return orderService.saveOrder(sourceOrder);
    }

    private Order updateSourceAndTargetOrder(Order sourceOrder, Order targetOrder, String sourceLineItemId) {

        final OrderLineItem sourceOrderLineItem = sourceOrder.getOrderLineItem(sourceLineItemId);

        targetOrder.findOrderLineItem(sourceLineItemId).ifPresentOrElse(li -> {
            targetOrder.productSetOrder().updateOrderLineItem(li, l -> l.incrementQuantity(1));
            sourceOrder.productSetOrder().updateOrderLineItem(sourceLineItemId, l -> l.decrementQuantity(1));

        }, () -> {
            targetOrder.productSetOrder().addSplitOrderLineItem(sourceOrderLineItem, sourceOrder);
        });

        orderService.saveOrder(sourceOrder);

        return orderService.saveOrder(targetOrder);
    }

    @Override
    public List<SplitAmountDetails> splitByHeadCount(String sourceOrderId, Integer headcount) {

        final Order sourceOrder = orderService.getOrder(sourceOrderId);

        List<SplitAmountDetails> splitAmountDetails = new ArrayList<>();
        AtomicInteger settledCount = new AtomicInteger(0);

        final BigDecimal settledTotal = orderTransactionService.getOrderTransactionByOrderId(sourceOrder.getId()).stream()
                .map(ot -> new SplitAmountDetails(ot.getSettleAmount(), true))
                .peek(splitAmountDetails::add)
                .peek(d -> settledCount.incrementAndGet())
                .map(SplitAmountDetails::getSplitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        final BigDecimal dividend = sourceOrder.getOrderTotal().subtract(settledTotal);

        if (dividend.compareTo(BigDecimal.ZERO) > 0) {
            int splitHeadCount = headcount - settledCount.intValue();

            if (splitHeadCount <= 0) {
                throw new BusinessLogicException("message.incorrectHeadCount", "Split head count must include the paid split order.");
            }

            final BigDecimal divisor = BigDecimal.valueOf(splitHeadCount);
            final BigDecimal[] result = dividend.divideAndRemainder(divisor);
            final BigDecimal splitAmount = result[0];
            final BigDecimal remainder = result[1];

            for (int i = 0; i < splitHeadCount - 1; i++) {
                splitAmountDetails.add(new SplitAmountDetails(splitAmount, false));
            }

            final BigDecimal lastSplitAmount = sourceOrder.deduceRoundingAmount(() -> splitAmount.add(remainder));
            splitAmountDetails.add(new SplitAmountDetails(lastSplitAmount, false));

            // save head count last after we made necessary check.
            sourceOrder.addMetadata(Order.HEAD_COUNT, headcount);
            orderService.saveOrder(sourceOrder);
        }

        return splitAmountDetails;
    }

    @Override
    public List<SplitAmountDetails> getSplitByHeadCount(String sourceOrderId) {

        final Order sourceOrder = orderService.getOrder(sourceOrderId);
        Integer headCount;

        if ((headCount = (Integer) sourceOrder.getMetadata(Order.HEAD_COUNT)) == null) {
            return List.of();
        }

        return this.splitByHeadCount(sourceOrderId, headCount);
    }

    @Override
    public void removeSplitByHeadCount(String sourceOrderId) {

        final Order sourceOrder = orderService.getOrder(sourceOrderId);
        final Object headCount = sourceOrder.getMetadata(Order.HEAD_COUNT);

        if (headCount != null) {
            if (!orderTransactionService.getOrderTransactionByOrderId(sourceOrder.getId()).isEmpty()) {
                throw new BusinessLogicException("message.cannotRemoveSplitByHeadCount", "The split order by headcount has at least one OrderTransaction, cannot remove.");
            }

            sourceOrder.removeMetadata(Order.HEAD_COUNT);
            orderService.saveOrder(sourceOrder);
        }
    }

}
