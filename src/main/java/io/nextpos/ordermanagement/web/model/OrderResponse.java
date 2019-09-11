package io.nextpos.ordermanagement.web.model;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.ordermanagement.data.TaxableAmount;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private String orderId;

    private String tableId;

    private Date createdDate;

    private Date modifiedDate;

    private Order.OrderState state;

    private TaxableAmount total;

    private BigDecimal serviceCharge;

    private BigDecimal orderTotal;

    private Currency currency;

    private List<OrderLineItemResponse> lineItems;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderLineItemResponse {

        private String lineItemId;

        private String productName;

        private BigDecimal price;
        
        private int quantity;

        private TaxableAmount subTotal;

        private OrderLineItem.LineItemState state;

        private String options;
    }
}
