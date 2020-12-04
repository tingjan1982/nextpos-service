package io.nextpos.ordertransaction.service;

import io.nextpos.client.data.Client;
import io.nextpos.einvoice.common.invoice.ElectronicInvoice;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordertransaction.data.OrderTransaction;

import java.util.List;

public interface ElectronicInvoiceService {

    String INVOICE_NUMBER_MISSING = "INVOICE_NUMBER_MISSING";

    boolean checkElectronicInvoiceEligibility(Client client);

    ElectronicInvoice createElectronicInvoice(Client client, Order order, OrderTransaction orderTransaction);

    void issueNewInvoiceNumber(ElectronicInvoice electronicInvoice);

    ElectronicInvoice getElectronicInvoice(String id);

    ElectronicInvoice getElectronicInvoiceByInvoiceNumber(String internalInvoiceNumber);

    List<ElectronicInvoice> getElectronicInvoicesByInvoiceStatus(Client client, ElectronicInvoice.InvoiceStatus invoiceStatus);

    void cancelElectronicInvoice(ElectronicInvoice electronicInvoice);

    void voidElectronicInvoice(ElectronicInvoice electronicInvoice);
}
