package io.nextpos.ordermanagement.web.model;

import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.TaxableAmount;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class OrdersByRangeResponse {

    private ZonedDateRange dateRange;

    private BigDecimal ordersTotal;

    private List<LightOrderResponse> orders;

    public OrdersByRangeResponse(ZonedDateRange dateRange, List<Order> orders) {

        this.dateRange = dateRange;
        this.ordersTotal = orders.stream()
                .filter(o -> o.getState() != Order.OrderState.DELETED)
                .map(Order::getOrderTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.orders = orders.stream()
                .map(LightOrderResponse::new)
                .collect(Collectors.toList());
    }

    @Data
    @AllArgsConstructor
    public static class LightOrderResponse {

        private String orderId;

        private String serialId;

        private Order.OrderType orderType;

        private Date createdTime;

        private Order.OrderState state;

        private TaxableAmount total;

        private BigDecimal orderTotal;

        public LightOrderResponse(Order order) {

            orderId = order.getId();
            serialId = order.getSerialId();
            orderType = order.getOrderType();
            createdTime = order.getCreatedDate();
            state = order.getState();
            total = order.getTotal();
            orderTotal = order.getOrderTotal();
        }
    }
}
