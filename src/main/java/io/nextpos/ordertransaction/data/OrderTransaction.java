package io.nextpos.ordertransaction.data;

import io.nextpos.einvoice.common.invoice.ElectronicInvoice;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.shared.model.MongoBaseObject;
import io.nextpos.shared.model.WithClientId;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class OrderTransaction extends MongoBaseObject implements WithClientId {

    @Id
    private String id;

    /**
     * This greatly improves the lookup aggregation performance.
     */
    @Indexed
    private String orderId;

    private String clientId;

    private OrderTransactionStatus status = OrderTransactionStatus.CREATED;

    private BigDecimal orderTotal;

    private BigDecimal settleAmount;

    private PaymentDetails paymentDetails;

    private InvoiceDetails invoiceDetails;

    private BillDetails billDetails;

    public OrderTransaction(Order order, PaymentMethod paymentMethod, BillType billType, BigDecimal settleAmount) {

        this.orderId = order.getId();
        this.clientId = order.getClientId();
        this.orderTotal = order.getOrderTotal();
        this.settleAmount = deduceSettleAmount(order, billType, settleAmount);
        this.paymentDetails = new PaymentDetails(paymentMethod);
        this.invoiceDetails = new InvoiceDetails();

        this.updateBillingDetails(order, billType);
    }

    private BigDecimal deduceSettleAmount(Order order, BillType billType, BigDecimal settleAmount) {

        if (billType == BillType.SINGLE) {
            return order.getOrderTotal();
        } else {
            return settleAmount;
        }
    }

    public void updateBillingDetails(Order order, BillType billType) {
        this.billDetails = new BillDetails(billType, createBillLineItems(order, billType, settleAmount));
    }

    private List<BillLineItem> createBillLineItems(Order order, BillType billType, BigDecimal settleAmount) {

        switch (billType) {
            case SINGLE:
                final List<OrderTransaction.BillLineItem> billLIneItems = order.getOrderLineItems().stream()
                        .map(li -> new OrderTransaction.BillLineItem(li.getProductSnapshot().getName(),
                                li.getQuantity(),
                                li.getProductPriceWithOptions().getAmountWithTax(),
                                li.getDeducedSubTotal().getAmountWithTax()))
                        .collect(Collectors.toList());

                if (order.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
                    final BigDecimal discount = order.deduceRoundingAmount(() -> order.getDiscount().negate());
                    billLIneItems.add(new OrderTransaction.BillLineItem("discount", 1, discount, discount));
                }

                if (order.getServiceCharge().compareTo(BigDecimal.ZERO) > 0) {
                    final BigDecimal serviceCharge = order.deduceRoundingAmount(order::getServiceCharge);
                    billLIneItems.add(new OrderTransaction.BillLineItem("serviceCharge", 1, serviceCharge, serviceCharge));
                }

                return billLIneItems;

            case SPLIT:
                return List.of(new OrderTransaction.BillLineItem("split", 1, settleAmount, settleAmount));
            default:
                return List.of();
        }
    }

    public String getPaymentMethod() {
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
    @NoArgsConstructor
    public static class PaymentDetails {

        private String paymentMethod;

        private PaymentStatus paymentStatus;

        private Map<OrderTransaction.PaymentDetailsKey, Object> values = new HashMap<>();

        PaymentDetails(final PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod.name();
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
    @AllArgsConstructor
    public static class BillDetails {

        private BillType billType;

        private List<BillLineItem> billLineItems;
    }

    @Data
    @AllArgsConstructor
    public static class BillLineItem {

        private String name;

        private int quantity;

        private BigDecimal unitPrice;

        private BigDecimal subTotal;
    }

    /**
     * Serves as validation check
     */
    public enum PaymentMethod {
        CASH, CARD, LINE_PAY, JKO, UBER_EATS, FOOD_PANDA
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
