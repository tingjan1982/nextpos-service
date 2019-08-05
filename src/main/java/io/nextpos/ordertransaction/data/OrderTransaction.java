package io.nextpos.ordertransaction.data;

import io.nextpos.shared.model.MongoBaseObject;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class OrderTransaction extends MongoBaseObject {

    @Id
    private String id;

    private String orderId;

    private BigDecimal orderTotal;

    private BigDecimal settleAmount;

    private PaymentMethodDetails paymentMethodDetails;

    private InvoiceDetails invoiceDetails;

    private BillDetails billDetails;

    public OrderTransaction(final String orderId,
                            final BigDecimal orderTotal,
                            final BigDecimal settleAmount,
                            final PaymentMethod paymentMethod,
                            final BillType billType) {
        this.orderId = orderId;
        this.orderTotal = orderTotal;
        this.settleAmount = settleAmount;
        this.paymentMethodDetails = new PaymentMethodDetails(paymentMethod);
        this.invoiceDetails = new InvoiceDetails();
        this.billDetails = new BillDetails(billType);
    }


    @Override
    public boolean isNew() {
        return id == null;
    }

    @Data
    public static class PaymentMethodDetails {

        private PaymentMethod paymentMethod;

        private PaymentStatus paymentStatus;

        private Map<String, String> details;

        PaymentMethodDetails(final PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
        }


    }

    @Data
    public static class InvoiceDetails {

        /**
         * this stores the e-invoice number
         */
        private String invoiceNumber;

        private String carrierType;
    }

    @Data
    public static class BillDetails {

        private BillType billType;

        /**
         * This is only applicable for CUSTOM BillType.
         */
        private List<BillLineItem> billLineItems;

        BillDetails(final BillType billType) {
            this.billType = billType;
        }
    }

    @Data
    public static class BillLineItem {

        private String name;

        private int quantity;

        private BigDecimal subTotal;
    }


    public enum PaymentMethod {
        CASH, CARD
    }

    public enum PaymentStatus {
        SUCCESS
    }

    public enum BillType {
        /**
         * Single bill
         */
        SINGLE,

        /**
         * Split bill equally amongst participants.
         */
        SPLIT,

        /**
         * Split bill by order line items. (i.e. pay for what one ordered)
         */
        CUSTOM
    }
}
