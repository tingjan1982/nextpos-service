package io.nextpos.ordertransaction.service;

import io.nextpos.ordertransaction.data.OrderTransaction;

public interface OrderTransactionService {

    OrderTransaction createOrderTransaction(OrderTransaction orderTransaction);
}
