package io.nextpos.ordertransaction.web.model;

import io.nextpos.einvoice.common.invoice.ElectronicInvoice;
import io.nextpos.ordertransaction.data.OrderTransaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Data
@RequiredArgsConstructor
public class OrderTransactionResponse {

    private final String transactionId;

    private final String orderId;

    private final OrderTransaction.BillType billType;

    private final BigDecimal orderTotal;

    private final BigDecimal settleAmount;

    private final String paymentMethod;

    private final OrderTransaction.PaymentDetails paymentDetails;

    private final List<BillLineItemResponse> billLineItems;

    private String invoiceNumber;

    private String buyerUbn;

    private ElectronicInvoice.InvoiceStatus invoiceStatus;

    private String receiptXML;

    private String invoiceXML;

    public static OrderTransactionResponse toOrderTransactionResponse(final OrderTransaction orderTransaction) {

        final List<OrderTransactionResponse.BillLineItemResponse> billLineItems = orderTransaction.getBillDetails().getBillLineItems().stream()
                .map(li -> new OrderTransactionResponse.BillLineItemResponse(li.getName(), li.getQuantity(), li.getSubTotal()))
                .collect(Collectors.toList());

        final OrderTransactionResponse response = new OrderTransactionResponse(orderTransaction.getId(),
                orderTransaction.getOrderId(),
                orderTransaction.getBillDetails().getBillType(),
                orderTransaction.getOrderTotal(),
                orderTransaction.getSettleAmount(),
                orderTransaction.getPaymentMethod(),
                orderTransaction.getPaymentDetails(),
                billLineItems);

        orderTransaction.getElectronicInvoice().ifPresent(electronicInvoice -> {
            response.setInvoiceNumber(electronicInvoice.getInternalInvoiceNumber());
            response.setBuyerUbn(electronicInvoice.getBuyerUbn());
            response.setInvoiceStatus(electronicInvoice.getInvoiceStatus());
        });

        return response;
    }

    @Data
    @AllArgsConstructor
    public static class BillLineItemResponse {

        private String name;

        private int quantity;

        private BigDecimal subTotal;
    }
}
