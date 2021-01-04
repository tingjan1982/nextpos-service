package io.nextpos.ordermanagement.data;

import io.nextpos.shared.exception.BusinessLogicException;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ProductSetOrder implements OrderLineItemOperation {

    private final Order order;

    private ProductSetOrder(Order order) {
        this.order = order;
    }

    static ProductSetOrder newInstance(Order order) {
        return new ProductSetOrder(order);
    }

    @Override
    public void addOrderLineItem(OrderLineItem orderLineItem) {

        order.addOrderLineItem(orderLineItem);

        if (orderLineItem.hasChildLineItems()) {
            orderLineItem.getChildLineItems().forEach(cli -> {
                cli.setAssociatedLineItemId(orderLineItem.getId());
                order.addOrderLineItem(cli);
            });
        }
    }

    @Override
    public void addSplitOrderLineItem(OrderLineItem sourceOrderLineItem, Order sourceOrder) {

        checkLineItemInSet(sourceOrderLineItem);
        order.addSplitOrderLineItem(sourceOrderLineItem, sourceOrder);

        final List<Runnable> addOps = sourceOrder.getOrderLineItems().stream()
                .filter(li -> sourceOrderLineItem.getId().equals(li.getAssociatedLineItemId()))
                .map(li -> (Runnable) () -> order.addSplitOrderLineItem(li, sourceOrder))
                .collect(Collectors.toList());

        addOps.forEach(Runnable::run);
    }

    @Override
    public void updateOrderLineItem(String lineItemId, Consumer<OrderLineItem> updateOperation) {

        final OrderLineItem orderLineItem = order.getOrderLineItem(lineItemId);
        this.updateOrderLineItem(orderLineItem, updateOperation);
    }

    @Override
    public void updateOrderLineItem(OrderLineItem orderLineItem, Consumer<OrderLineItem> updateOperation) {

        checkLineItemInSet(orderLineItem);
        order.updateOrderLineItem(orderLineItem, updateOperation);

        final List<Runnable> updateOps = order.getOrderLineItems().stream()
                .filter(li -> orderLineItem.getId().equals(li.getAssociatedLineItemId()))
                .map(li -> (Runnable) () -> order.updateOrderLineItem(li, updateOperation))
                .collect(Collectors.toList());

        updateOps.forEach(Runnable::run);
    }

    /**
     * Removes associated line items that were created as part of this line item.
     */
    @Override
    public void deleteOrderLineItem(OrderLineItem orderLineItem) {

        checkLineItemInSet(orderLineItem);

        order.getOrderLineItems().removeIf(li -> orderLineItem.getId().equals(li.getAssociatedLineItemId()));

        order.deleteOrderLineItem(orderLineItem);
    }

    private void checkLineItemInSet(OrderLineItem orderLineItem) {

        if (StringUtils.isNotBlank(orderLineItem.getAssociatedLineItemId())) {
            throw new BusinessLogicException("message.operationNotAllowed", "Cannot perform update line item on set line items.");
        }
    }
}
