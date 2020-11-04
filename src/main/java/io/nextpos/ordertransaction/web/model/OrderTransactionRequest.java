package io.nextpos.ordertransaction.web.model;

import io.nextpos.einvoice.common.invoice.ElectronicInvoice;
import io.nextpos.ordertransaction.data.OrderTransaction;
import io.nextpos.ordertransaction.web.model.validator.ValidBillTypeDetails;
import io.nextpos.shared.model.validator.ValidEnum;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@ValidBillTypeDetails
public class OrderTransactionRequest {

    @NotEmpty
    private String orderId;

    @ValidEnum(enumType = OrderTransaction.PaymentMethod.class)
    private String paymentMethod;

    @ValidEnum(enumType = OrderTransaction.BillType.class)
    private String billType;

    @Positive
    private BigDecimal settleAmount;

    private String taxIdNumber;

    private ElectronicInvoice.CarrierType carrierType;

    private String carrierId;

    private String npoBan;

    private boolean printMark;

    private Map<OrderTransaction.PaymentDetailsKey, Object> paymentDetails = new HashMap<>();
}
