package io.nextpos.ordertransaction.data;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class PaymentMethodTotal {

    private Object id;

    private OrderTransaction.OrderTransactionStatus status;

    private OrderTransaction.PaymentMethod paymentMethod;

    private BigDecimal orderTotal;

    private BigDecimal settleAmount;

    private BigDecimal serviceCharge;

    private BigDecimal discount;

    private int orderCount;
}
