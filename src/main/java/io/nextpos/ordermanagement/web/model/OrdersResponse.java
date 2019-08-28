package io.nextpos.ordermanagement.web.model;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.TaxableAmount;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class OrdersResponse {

    /**
     * Key is table id, value is order response.
     */
    private Map<String, LightOrderResponse> orders;

    @Data
    @AllArgsConstructor
    public static class LightOrderResponse {

        private String orderId;

        private Order.OrderState state;

        private TaxableAmount total;
    }
}
