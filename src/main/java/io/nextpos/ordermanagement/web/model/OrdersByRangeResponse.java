package io.nextpos.ordermanagement.web.model;

import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.TaxableAmount;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
public class OrdersByRangeResponse {

    private ZonedDateRange dateRange;

    private BigDecimal ordersTotal;

    private List<LightOrderResponse> orders;

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
    }
}
