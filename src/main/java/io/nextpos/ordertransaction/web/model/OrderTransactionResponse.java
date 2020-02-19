package io.nextpos.ordertransaction.web.model;

import io.nextpos.ordertransaction.data.OrderTransaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class OrderTransactionResponse {

    private String transactionId;

    private String orderId;

    private OrderTransaction.BillType billType;

    private BigDecimal orderTotal;

    private BigDecimal settleAmount;

    private OrderTransaction.PaymentMethod paymentMethod;

    private BigDecimal cashChange;

    private List<BillLineItemResponse> billLineItems;

    private String orderDetailsPrintInstruction;

    public static OrderTransactionResponse toOrderTransactionResponse(final OrderTransaction orderTransaction, final String orderDetailsPrintInstruction) {

        final List<OrderTransactionResponse.BillLineItemResponse> billLineItems = orderTransaction.getBillDetails().getBillLineItems().stream()
                .map(li -> new OrderTransactionResponse.BillLineItemResponse(li.getName(), li.getQuantity(), li.getSubTotal()))
                .collect(Collectors.toList());

        return new OrderTransactionResponse(orderTransaction.getId(),
                orderTransaction.getOrderId(),
                orderTransaction.getBillDetails().getBillType(),
                orderTransaction.getOrderTotal(),
                orderTransaction.getSettleAmount(),
                orderTransaction.getPaymentMethod(),
                orderTransaction.getCashChange(),
                billLineItems,
                orderDetailsPrintInstruction);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BillLineItemResponse {

        private String name;

        private int quantity;

        private BigDecimal subTotal;
    }
}
