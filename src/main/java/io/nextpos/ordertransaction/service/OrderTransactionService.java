package io.nextpos.ordertransaction.service;

import io.nextpos.client.data.Client;
import io.nextpos.ordertransaction.data.OrderTransaction;

import java.util.List;

public interface OrderTransactionService {

    OrderTransaction createOrderTransaction(final Client client, OrderTransaction orderTransaction);

    OrderTransaction getOrderTransaction(String id);

    List<OrderTransaction> getOrderTransactionByOrderId(String orderId);
}
