package io.nextpos.ordertransaction.service;

import io.nextpos.client.data.Client;
import io.nextpos.ordertransaction.data.OrderTransaction;

public interface OrderTransactionService {

    OrderTransaction createOrderTransaction(OrderTransaction orderTransaction);

    String createOrderDetailsPrintInstruction(Client client, OrderTransaction orderTransaction);

    OrderTransaction getOrderTransaction(String id);
}
