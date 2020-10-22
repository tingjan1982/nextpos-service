package io.nextpos.subscription.web.model;

import io.nextpos.subscription.data.ClientSubscriptionInvoice;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class ClientSubscriptionInvoiceResponse {

    private String id;

    private String invoiceIdentifier;

    private ClientSubscriptionInvoice.SubscriptionInvoiceStatus status;

    private Date validFrom;

    private Date validTo;

    private Date paymentDate;
}
