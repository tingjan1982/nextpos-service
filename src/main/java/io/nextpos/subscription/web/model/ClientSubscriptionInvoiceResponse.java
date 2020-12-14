package io.nextpos.subscription.web.model;

import io.nextpos.subscription.data.ClientSubscriptionInvoice;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
public class ClientSubscriptionInvoiceResponse {

    private String id;

    private String invoiceIdentifier;

    private BigDecimal dueAmount;

    private ClientSubscriptionInvoice.SubscriptionInvoiceStatus status;

    private Date validFrom;

    private Date validTo;

    private Date paymentDate;

    public ClientSubscriptionInvoiceResponse(ClientSubscriptionInvoice invoice) {
        id = invoice.getId();
        invoiceIdentifier = invoice.getInvoiceIdentifier();
        dueAmount = invoice.getDueAmount().getAmount();
        status = invoice.getStatus();
        validFrom = invoice.getValidFrom();
        validTo = invoice.getValidTo();
        paymentDate = invoice.getPaymentDate();
    }
}
