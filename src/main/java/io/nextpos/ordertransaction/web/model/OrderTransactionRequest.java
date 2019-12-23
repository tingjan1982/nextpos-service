package io.nextpos.ordertransaction.web.model;

import io.nextpos.ordertransaction.data.OrderTransaction;
import io.nextpos.ordertransaction.web.model.validator.ValidBillTypeDetails;
import io.nextpos.shared.model.validator.ValidEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ValidBillTypeDetails
public class OrderTransactionRequest {

    @NotEmpty
    private String orderId;

    @ValidEnum(enumType = OrderTransaction.PaymentMethod.class)
    private String paymentMethod;

    @ValidEnum(enumType = OrderTransaction.BillType.class)
    private String billType;

    private Integer splitWith;

    /**
     * The amount of cash provided by the customer to settle this transaction.
     */
    @PositiveOrZero
    private BigDecimal cash;

    @Valid
    private List<BillLineItemRequest> billLineItems;

    @Data
    public static class BillLineItemRequest {

        @NotEmpty
        private String lineItemId;

        @Positive
        private int quantity;
    }
}
