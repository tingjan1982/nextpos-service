package io.nextpos.ordertransaction.data;

import io.nextpos.shared.model.MongoBaseObject;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
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

    private PaymentDetails paymentDetails;

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
        this.paymentDetails = new PaymentDetails(paymentMethod);
        this.invoiceDetails = new InvoiceDetails();
        this.billDetails = new BillDetails(billType);
        this.billDetails.getBillLineItems().addAll(billLineItems);
    }

    public PaymentMethod getPaymentMethod() {
        return this.paymentDetails.getPaymentMethod();
    }

    public void setTaxIdNumber(String taxIdNumber) {
        this.invoiceDetails.setTaxIdNumber(taxIdNumber);
    }

    public void putPaymentDetails(PaymentDetailsKey key, Object value) {
        paymentDetails.values.put(key, value);
    }

    public <T> T getPaymentDetailsByKey(PaymentDetailsKey key) {
        return (T) paymentDetails.values.get(key);
    }


    @Override
    public boolean isNew() {
        return id == null;
    }

    @Data
    public static class PaymentDetails {

        private PaymentMethod paymentMethod;

        private PaymentStatus paymentStatus;

        private Map<OrderTransaction.PaymentDetailsKey, Object> values = new HashMap<>();

        PaymentDetails(final PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
        }
    }

    public enum PaymentDetailsKey {

        CASH(BigDecimal.class), CASH_CHANGE(BigDecimal.class), CARD_TYPE, LAST_FOUR_DIGITS;

        private final Class<?> valueType;

        PaymentDetailsKey() {
            valueType = String.class;
        }

        PaymentDetailsKey(final Class<?> valueType) {
            this.valueType = valueType;
        }

        public Class<?> getValueType() {
            return valueType;
        }
    }



    @Data
    public static class InvoiceDetails {

        private String taxIdNumber;

        private boolean needElectronicInvoice;

        @DBRef
        private ElectronicInvoice electronicInvoice;
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

        private BigDecimal unitPrice;

        private BigDecimal subTotal;
    }


    public enum PaymentMethod {
        CASH, CARD
    }

    public enum PaymentStatus {
        SUCCESS, FAIL
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
