package io.nextpos.ordertransaction.data;

import io.nextpos.einvoice.common.invoice.ElectronicInvoice;
import io.nextpos.shared.model.MongoBaseObject;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.*;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class OrderTransaction extends MongoBaseObject {

    @Id
    private String id;

    private String orderId;

    private String clientId;

    private OrderTransactionStatus status;

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
        this.status = OrderTransactionStatus.CREATED;
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

    public void updateInvoiceDetails(String taxIdNumber, ElectronicInvoice.CarrierType carrierType, String carrierId, String npoBan, boolean printMark) {
        invoiceDetails.setTaxIdNumber(taxIdNumber);
        invoiceDetails.setCarrierType(carrierType);
        invoiceDetails.setCarrierId(carrierId);
        invoiceDetails.setCarrierId2(carrierId);
        invoiceDetails.setNpoBan(npoBan);
        invoiceDetails.setPrintMark(printMark);
    }

    public void updateElectronicInvoiceOptionalDetails(ElectronicInvoice electronicInvoice) {
        electronicInvoice.setBuyerUbn(invoiceDetails.getTaxIdNumber());
        electronicInvoice.setCarrierType(invoiceDetails.getCarrierType());
        electronicInvoice.setCarrierId(invoiceDetails.getCarrierId());
        electronicInvoice.setCarrierId2(invoiceDetails.getCarrierId2());
        electronicInvoice.setNpoBan(invoiceDetails.getNpoBan());
        electronicInvoice.setPrintMark(invoiceDetails.isPrintMark());
    }

    public void putPaymentDetails(PaymentDetailsKey key, Object value) {
        paymentDetails.values.put(key, value);
    }

    public <T> T getPaymentDetailsByKey(PaymentDetailsKey key) {
        return (T) paymentDetails.values.get(key);
    }

    public Optional<ElectronicInvoice> getElectronicInvoice() {
        return Optional.ofNullable(invoiceDetails.getElectronicInvoice());
    }

    public boolean hasElectronicInvoice() {
        return getElectronicInvoice().isPresent();
    }

    public boolean hasPrintableElectronicInvoice() {
        return hasElectronicInvoice() && invoiceDetails.getElectronicInvoice().canPrintElectronicInvoice();
    }

    public enum OrderTransactionStatus {

        CREATED,
        CANCELLED,
        VOIDED
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

        private ElectronicInvoice.CarrierType carrierType;

        private String carrierId;

        private String carrierId2;

        private String npoBan;

        private boolean printMark;

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
         * Single bill - applies to single order and split order.
         */
        SINGLE,

        /**
         * Split order by head count.
         */
        SPLIT,
    }
}
