package io.nextpos.ordertransaction.service;

import io.nextpos.client.data.Client;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordertransaction.data.ElectronicInvoice;
import io.nextpos.ordertransaction.data.OrderTransaction;

public interface ElectronicInvoiceService {

    ElectronicInvoice createElectronicInvoice(Client client, Order order, OrderTransaction orderTransaction);
}
