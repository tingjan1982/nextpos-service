package io.nextpos.ordermanagement.data;

import java.util.function.Consumer;

public interface OrderLineItemOperation {

    void addOrderLineItem(OrderLineItem orderLineItem);

    void addSplitOrderLineItem(OrderLineItem sourceOrderLineItem, Order sourceOrder);

    void updateOrderLineItem(String lineItemId, Consumer<OrderLineItem> updateOperation);

    void updateOrderLineItem(OrderLineItem orderLineItem, Consumer<OrderLineItem> updateOperation);

    void deleteOrderLineItem(OrderLineItem orderLineItem);
}
