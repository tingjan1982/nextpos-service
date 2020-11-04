package io.nextpos.ordertransaction.service;

import io.nextpos.client.data.Client;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordertransaction.data.OrderTransaction;

import java.util.List;

public interface OrderTransactionService {

    OrderTransaction createOrderTransaction(final Client client, OrderTransaction orderTransaction);

    OrderTransaction getOrderTransaction(String id);

    Order getOrderByInvoiceNumber(String invoiceNumber);

    List<OrderTransaction> getOrderTransactionByOrderId(String orderId);

    void cancelOrderTransaction(String id);

    void voidOrderTransaction(String id);
}
