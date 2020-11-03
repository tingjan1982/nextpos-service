package io.nextpos.ordertransaction.service;

import io.nextpos.client.data.Client;
import io.nextpos.einvoice.common.invoice.ElectronicInvoice;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordertransaction.data.OrderTransaction;

public interface ElectronicInvoiceService {

    String INVOICE_NUMBER_MISSING = "INVOICE_NUMBER_MISSING";

    boolean checkElectronicInvoiceEligibility(Client client);

    ElectronicInvoice createElectronicInvoice(Client client, Order order, OrderTransaction orderTransaction);

    ElectronicInvoice getElectronicInvoiceByInvoiceNumber(String internalInvoiceNumber);

    void cancelElectronicInvoice(ElectronicInvoice electronicInvoice);
}
