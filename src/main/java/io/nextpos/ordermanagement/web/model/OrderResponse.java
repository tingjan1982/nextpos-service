package io.nextpos.ordermanagement.web.model;

import io.nextpos.merchandising.data.OfferApplicableObject;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderDuration;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.ordermanagement.data.TaxableAmount;
import io.nextpos.ordertransaction.web.model.OrderTransactionResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
//@AllArgsConstructor
@RequiredArgsConstructor
public class OrderResponse {

    private final String orderId;

    private final String serialId;

    private final Order.OrderType orderType;

    private final Order.TableInfo tableInfo;

    private final String tableNote;

    private final String tableDisplayName;

    private final String servedBy;

    private final Date createdDate;

    private final Date modifiedDate;

    private final Order.OrderState state;

    private final TaxableAmount total;

    private final TaxableAmount discountedTotal;

    private final BigDecimal discount;

    private final BigDecimal serviceCharge;

    private final BigDecimal orderTotal;

    private final Currency currency;

    private final List<OrderLineItemResponse> lineItems;

    private final Map<String, Object> metadata;

    private final Order.DemographicData demographicData;

    private final OfferApplicableObject.AppliedOfferInfo appliedOfferInfo;

    private final OrderDuration orderDuration;

    private OrderDuration orderPreparationTime;

    private List<OrderTransactionResponse> transactions;


    @Data
    @AllArgsConstructor
    public static class OrderLineItemResponse {

        private String lineItemId;

        private String productId;

        private OrderLineItem.LineItemState state;

        private String productName;

        private String options;

        private BigDecimal price;

        private int quantity;

        private TaxableAmount subTotal;

        private TaxableAmount discountedSubTotal;

        private OfferApplicableObject.AppliedOfferInfo appliedOfferInfo;

        private Date modifiedDate;
    }
}
