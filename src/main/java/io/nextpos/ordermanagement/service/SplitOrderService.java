package io.nextpos.ordermanagement.service;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.SplitAmountDetails;

import java.util.List;

public interface SplitOrderService {

    Order newSplitOrder(String sourceOrderId, String sourceLineItemId);

    Order updateLineItem(String sourceOrderId, String targetOrderId, String sourceLineItemId);

    List<SplitAmountDetails> splitByHeadCount(String sourceOrderId, Integer headcount);
}
