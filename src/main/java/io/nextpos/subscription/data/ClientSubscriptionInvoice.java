package io.nextpos.subscription.data;

import io.nextpos.einvoice.common.invoice.ElectronicInvoice;
import io.nextpos.ordermanagement.data.TaxableAmount;
import io.nextpos.shared.model.MongoBaseObject;
import io.nextpos.shared.model.WithClientId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ClientSubscriptionInvoice extends MongoBaseObject implements WithClientId {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Id
    private String id;

    private String clientId;

    @DBRef
    private ClientSubscription clientSubscription;

    /**
     * Used to easily look up the invoice to activate the client subscription.
     */
    private String invoiceIdentifier;

    private ZoneId zoneId;

    private Date validFrom;

    private Date validTo;

    /**
     * Can consist of multiple subscriptions.
     */
    private TaxableAmount dueAmount;

    private Date dueDate;

    private SubscriptionInvoiceStatus status;

    private Date paymentDate;

    private boolean invoiceSent;

    @DBRef
    private ElectronicInvoice electronicInvoice;

    public ClientSubscriptionInvoice(ZoneId zoneId, ClientSubscription clientSubscription, Date validFrom) {

        this.clientId = clientSubscription.getClientId();
        this.clientSubscription = clientSubscription;
        this.invoiceIdentifier = RandomStringUtils.randomNumeric(6);
        this.zoneId = zoneId;

        this.validFrom = validFrom;
        final int numberOfMonths = clientSubscription.getPlanPeriod().getNumberOfMonths();
        final ZonedDateTime zonedValidTo = validFrom.toInstant().atZone(zoneId).plusMonths(numberOfMonths);
        this.validTo = Date.from(zonedValidTo.toInstant());

        this.dueAmount = clientSubscription.getSubscriptionPlanSnapshot().getTaxableAmount().newInstance();
        final BigDecimal discountAmount = clientSubscription.getDiscountAmount();
        final BigDecimal calculatedAmount = clientSubscription.getPlanPrice().subtract(discountAmount);
        this.dueAmount.calculate(calculatedAmount);
        this.dueDate = validFrom;

        this.status = SubscriptionInvoiceStatus.PENDING;
    }

    public void updatePaymentStatus(Date paidDate) {

        setPaymentDate(paidDate);
        setStatus(ClientSubscriptionInvoice.SubscriptionInvoiceStatus.PAID);
    }

    public String getSubscriptionPeriod(ZoneId zoneId) {
        final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        df.setTimeZone(TimeZone.getTimeZone(zoneId));

        return df.format(validFrom) + " - " + df.format(validTo);
    }

    public enum SubscriptionInvoiceStatus {

        /**
         * Invoice created and not paid.
         */
        PENDING,

        /**
         * Invoice is paid.
         */
        PAID,

        /**
         * Invoice is unpaid and passed due date.
         */
        OVERDUE,

        CANCELLED
    }
}
