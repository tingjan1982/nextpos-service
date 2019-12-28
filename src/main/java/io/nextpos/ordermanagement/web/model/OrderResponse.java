package io.nextpos.ordermanagement.web.model;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.ordermanagement.data.TaxableAmount;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class OrderResponse {

    private String orderId;

    private String serialId;

    private Order.TableInfo tableInfo;

    private String servedBy;

    private Date createdDate;

    private Date modifiedDate;

    private Order.OrderState state;

    private TaxableAmount total;

    private TaxableAmount discountedTotal;

    private BigDecimal serviceCharge;

    private BigDecimal orderTotal;

    private Currency currency;

    private List<OrderLineItemResponse> lineItems;

    private Map<String, Object> metadata;

    private Order.DemographicData demographicData;

    @Data
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
