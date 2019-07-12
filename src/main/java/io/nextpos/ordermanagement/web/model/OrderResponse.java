package io.nextpos.ordermanagement.web.model;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.TaxableAmount;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private String orderId;

    private Order.OrderState state;

    private TaxableAmount total;

    private List<OrderLineItemResponse> lineItems;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderLineItemResponse {

        private String productName;

        private BigDecimal price;
        
        private int quantity;

        private TaxableAmount subTotal;

        private String options;
    }
}
