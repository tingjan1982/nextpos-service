package io.nextpos.ordertransaction.data;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Data
@NoArgsConstructor
public class ClosingShiftTransactionReport {

    private List<PaymentMethodTotal> totalByPaymentMethod;

    private List<OrderCount> totalOrderCount;

    private List<OrderCount> orderCountByState;

    public int getTotalOrderCount() {
        if (!CollectionUtils.isEmpty(totalOrderCount)) {
            return totalOrderCount.get(0).getOrderCount();
        }

        return 0;
    }

    public Optional<PaymentMethodTotal> getTotalByPaymentMethod(OrderTransaction.PaymentMethod paymentMethod) {

        return totalByPaymentMethod.stream()
                .filter(p -> p.getPaymentMethod() == paymentMethod)
                .findFirst();
    }

    @Data
    @NoArgsConstructor
    public static class PaymentMethodTotal {

        private String id;

        private OrderTransaction.PaymentMethod paymentMethod;

        private BigDecimal orderTotal;

        private BigDecimal serviceCharge;

        private BigDecimal discount;

        private int orderCount;
    }

    @Data
    @NoArgsConstructor
    public static class OrderCount {

        private String id;

        private int orderCount;
    }
}
