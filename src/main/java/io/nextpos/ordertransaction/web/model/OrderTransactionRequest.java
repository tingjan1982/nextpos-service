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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private String taxIdNumber;

    private Integer splitWith;

    @Valid
    private List<BillLineItemRequest> billLineItems;

    private Map<OrderTransaction.PaymentDetailsKey, Object> paymentDetails = new HashMap<>();

    private boolean needElectronicInvoice;

    @Data
    public static class BillLineItemRequest {

        @NotEmpty
        private String lineItemId;

        @Positive
        private int quantity;
    }
}
