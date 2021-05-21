package io.nextpos.ordertransaction.data;

import io.nextpos.ordermanagement.data.Order;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
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

    //private transient List<Object> entries;

    private List<OrderCount> totalOrderCount;

    private List<OrderCount> orderCountByState;

    public Map<String, PaymentMethodTotal> getTotalByPaymentMethod() {

        if (!CollectionUtils.isEmpty(totalByPaymentMethod)) {
            return totalByPaymentMethod.stream().collect(Collectors.toMap(PaymentMethodTotal::getPaymentMethod, p -> p));
        }

        return Map.of();
    }

    public OrderSummary getOneOrderSummary() {
        return CollectionUtils.isEmpty(orderSummary) ? new OrderSummary() : orderSummary.get(0);
    }

    public int getTotalOrderCount() {
        return !CollectionUtils.isEmpty(totalOrderCount) ? totalOrderCount.get(0).getOrderCount() : 0;
    }

    public Map<Order.OrderState, OrderCount> getOrderCountByState() {

        if (!CollectionUtils.isEmpty(orderCountByState)) {
            return orderCountByState.stream().collect(Collectors.toMap(OrderCount::getOrderState, oc -> oc));
        }

        return Map.of();
    }

    public OrderCount getOrderCount(Order.OrderState orderState) {
        return getOrderCountByState().getOrDefault(orderState, new OrderCount());
    }

    public Optional<PaymentMethodTotal> getPaymentMethodTotal(String paymentMethod) {
        return totalByPaymentMethod.stream()
                .filter(p -> StringUtils.equals(p.getPaymentMethod(), paymentMethod))
                .findFirst();
    }

    @Data
    @NoArgsConstructor
    public static class OrderSummary {

        private String id;

        private BigDecimal orderTotal = BigDecimal.ZERO;

        private BigDecimal settleAmount = BigDecimal.ZERO;

        private BigDecimal serviceCharge = BigDecimal.ZERO;

        private BigDecimal discount = BigDecimal.ZERO;

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
