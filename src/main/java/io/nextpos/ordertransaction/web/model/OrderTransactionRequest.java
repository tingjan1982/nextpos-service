package io.nextpos.ordertransaction.web.model;

import io.nextpos.ordertransaction.data.OrderTransaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderTransactionRequest {

    @NotEmpty
    private String orderId;

    @NotNull
    private OrderTransaction.PaymentMethod paymentMethod;

    @NotNull
    private OrderTransaction.BillType billType;

    private Integer splitWith;

    private List<BillLineItemRequest> billLineItems;

    @Data
    public static class BillLineItemRequest {

        private String lineItemId;

        private int quantity;
    }
}
