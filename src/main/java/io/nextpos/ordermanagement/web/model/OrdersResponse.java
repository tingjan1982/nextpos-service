package io.nextpos.ordermanagement.web.model;

import io.nextpos.ordermanagement.data.Order;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class OrdersResponse {

    /**
     * Key is table layout id, value is list of LightOrderResponse.
     */
    private Map<String, List<LightOrderResponse>> orders;

    @Data
    @AllArgsConstructor
    public static class LightOrderResponse {

        private String orderId;

        private String serialId;

        private String serialIdSuffix;

        private Order.OrderType orderType;

        private String tableLayoutId;

        private String tableLayout;

        private String tableName;

        private int customerCount;

        private Date createdTime;

        private Order.OrderState state;

        private BigDecimal orderTotal;
    }
}
