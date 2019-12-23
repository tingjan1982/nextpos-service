package io.nextpos.ordertransaction.web.model;

import io.nextpos.ordertransaction.data.OrderTransaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderTransactionResponse {

    private String transactionId;

    private String orderId;

    private String client;

    private String ubn;

    private BigDecimal orderTotal;

    private BigDecimal settleAmount;

    private BigDecimal cashChange;

    private OrderTransaction.BillType billType;

    private List<BillLineItemResponse> billLineItems;

    private String orderDetailsPrintInstruction;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BillLineItemResponse {

        private String name;

        private int quantity;

        private BigDecimal subTotal;
    }
}
