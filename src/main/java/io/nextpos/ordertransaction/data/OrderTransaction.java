package io.nextpos.ordertransaction.data;

import io.nextpos.shared.model.MongoBaseObject;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.ArrayList;
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

    private String clientId;

    private BigDecimal orderTotal;

    private BigDecimal settleAmount;

    private PaymentMethodDetails paymentMethodDetails;

    private InvoiceDetails invoiceDetails;

    private BillDetails billDetails;

    public OrderTransaction(final String orderId,
                            final String clientId,
                            final BigDecimal orderTotal,
                            final BigDecimal settleAmount,
                            final PaymentMethod paymentMethod,
                            final BillType billType,
                            final List<BillLineItem> billLineItems) {
        this.orderId = orderId;
        this.clientId = clientId;
        this.orderTotal = orderTotal;
        this.settleAmount = settleAmount;
        this.paymentMethodDetails = new PaymentMethodDetails(paymentMethod);
        this.invoiceDetails = new InvoiceDetails();
        this.billDetails = new BillDetails(billType);
        this.billDetails.getBillLineItems().addAll(billLineItems);
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

        private List<BillLineItem> billLineItems = new ArrayList<>();

        BillDetails(final BillType billType) {
            this.billType = billType;
        }
    }

    @Data
    @AllArgsConstructor
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
