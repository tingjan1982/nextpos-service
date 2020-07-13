package io.nextpos.ordertransaction.data;

import io.nextpos.ordermanagement.data.Order;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class ClosingShiftTransactionReport {

    private List<PaymentMethodTotal> totalByPaymentMethod;

    private List<OrderSummary> orderSummary;

    private List<OrderCount> totalOrderCount;

    private List<OrderCount> orderCountByState;

    public int getTotalOrderCount() {
        if (!CollectionUtils.isEmpty(totalOrderCount)) {
            return totalOrderCount.get(0).getOrderCount();
        }

        return 0;
    }

    public Map<OrderTransaction.PaymentMethod, PaymentMethodTotal> getTotalByPaymentMethod() {

        if (!CollectionUtils.isEmpty(totalByPaymentMethod)) {
            return totalByPaymentMethod.stream().collect(Collectors.toMap(PaymentMethodTotal::getPaymentMethod, p -> p));
        }

        return Map.of();
    }

    public Map<Order.OrderState, OrderCount> getOrderCountByState() {

        if (!CollectionUtils.isEmpty(orderCountByState)) {
            return orderCountByState.stream().collect(Collectors.toMap(OrderCount::getOrderState, oc -> oc));
        }

        return Map.of();
    }

    public Optional<PaymentMethodTotal> getTotalByPaymentMethod(OrderTransaction.PaymentMethod paymentMethod) {

        return totalByPaymentMethod.stream()
                .filter(p -> p.getPaymentMethod() == paymentMethod)
                .findFirst();
    }

    @Data
    @NoArgsConstructor
    public static class OrderSummary {

        private String id;

        private BigDecimal orderTotal;

        private BigDecimal settleAmount;

        private BigDecimal serviceCharge;

        private BigDecimal discount;

        private int orderCount;
    }

    @Data
    @NoArgsConstructor
    public static class PaymentMethodTotal {

        private String id;

        private OrderTransaction.PaymentMethod paymentMethod;

        private BigDecimal orderTotal;

        private BigDecimal settleAmount;

        private BigDecimal serviceCharge;

        private BigDecimal discount;

        private int orderCount;
    }

    @Data
    @NoArgsConstructor
    public static class OrderCount {

        private String id;

        private Order.OrderState orderState;

        private int orderCount;
    }
}
