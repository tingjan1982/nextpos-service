package io.nextpos.invoicenumber.web.model;

import io.nextpos.einvoice.common.invoice.ElectronicInvoice;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ElectronicInvoiceResponse {

    private String id;

    private String orderId;

    private String invoiceNumber;

    private ElectronicInvoice.InvoiceStatus invoiceStatus;

    public ElectronicInvoiceResponse(ElectronicInvoice electronicInvoice) {
        this.id = electronicInvoice.getId();
        this.orderId = electronicInvoice.getOrderId();
        this.invoiceNumber = electronicInvoice.getInvoiceNumber();
        this.invoiceStatus = electronicInvoice.getInvoiceStatus();
    }
}
