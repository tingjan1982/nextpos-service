package io.nextpos.subscription.data;

import io.nextpos.ordermanagement.data.TaxableAmount;
import io.nextpos.shared.model.MongoBaseObject;
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
public class ClientSubscriptionInvoice extends MongoBaseObject {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Id
    private String id;

    @DBRef
    private ClientSubscription clientSubscription;

    /**
     * Used to easily look up the invoice to activate the client subscription.
     */
    private String invoiceIdentifier;

    private Date validFrom;

    private Date validTo;

    /**
     * Can consist of multiple subscriptions.
     */
    private TaxableAmount dueAmount;

    private Date dueDate;

    private SubscriptionInvoiceStatus status;

    private Date paymentDate;


    public ClientSubscriptionInvoice(ZoneId zoneId, ClientSubscription clientSubscription, Date validFrom) {

        this.clientSubscription = clientSubscription;
        this.invoiceIdentifier = RandomStringUtils.randomNumeric(6);

        this.validFrom = validFrom;
        final int numberOfMonths = clientSubscription.getPlanPeriod().getNumberOfMonths();
        final ZonedDateTime zonedValidTo = validFrom.toInstant().atZone(zoneId).plusMonths(numberOfMonths);
        this.validTo = Date.from(zonedValidTo.toInstant());

        this.dueAmount = clientSubscription.getSubscriptionPlanSnapshot().getTaxableAmount().newInstance();
        this.dueAmount.calculate(clientSubscription.getPlanPrice().multiply(new BigDecimal(numberOfMonths)));
        this.dueDate = validFrom;

        this.status = SubscriptionInvoiceStatus.PENDING;
    }

    public String getSubscriptionPeriod(ZoneId zoneId) {
        final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM");
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
        OVERDUE
    }
}
