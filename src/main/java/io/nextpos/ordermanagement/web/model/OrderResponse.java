package io.nextpos.ordermanagement.web.model;

import io.nextpos.merchandising.data.OfferApplicableObject;
import io.nextpos.ordermanagement.data.*;
import io.nextpos.ordertransaction.web.model.OrderTransactionResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@RequiredArgsConstructor
public class OrderResponse {

    private final String orderId;

    private final String serialId;

    private final Order.OrderType orderType;

    private final Order.TableInfo tableInfo;

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

    private final List<OrderLog> orderLogs;

    private final Order.DemographicData demographicData;

    private final OfferApplicableObject.AppliedOfferInfo appliedOfferInfo;

    private final OrderDuration orderDuration;

    private OrderDuration orderPreparationTime;

    private List<OrderTransactionResponse> transactions;


    public static OrderResponse toOrderResponse(final Order order) {

        final List<OrderLineItemResponse> orderLineItems = order.getOrderLineItems().stream()
                .map(li -> {
                    final ProductSnapshot productSnapshot = li.getProductSnapshot();

                    return new OrderLineItemResponse(li.getId(),
                            productSnapshot.getId(),
                            li.getState(),
                            productSnapshot.getName(),
                            productSnapshot.getInternalName(),
                            li.getProductOptions(),
                            li.getProductPriceWithOptions().getAmount(),
                            li.getQuantity(),
                            li.getLineItemSubTotal(),
                            li.getSubTotal(),
                            li.getDiscountedSubTotal(),
                            li.getAppliedOfferInfo(),
                            li.getModifiedDate(),
                            productSnapshot.getChildProducts());

                }).collect(Collectors.toList());

        return new OrderResponse(order.getId(),
                order.getSerialId(),
                order.getOrderType(),
                order.getTableInfo(),
                order.getTableInfo().getDisplayName(),
                order.getServedBy(),
                order.getCreatedDate(),
                order.getModifiedDate(),
                order.getState(),
                order.getTotal(),
                order.getDiscountedTotal(),
                order.getDiscount(),
                order.getServiceCharge(),
                order.getOrderTotal(),
                order.getCurrency(),
                orderLineItems,
                order.getMetadata(),
                order.getOrderLogs(),
                order.getDemographicData(),
                order.getAppliedOfferInfo(),
                order.getOrderDuration());
    }


    @Data
    @AllArgsConstructor
    public static class OrderLineItemResponse {

        private String lineItemId;

        private String productId;

        private OrderLineItem.LineItemState state;

        private String productName;

        private String internalProductName;

        private String options;

        private BigDecimal price;

        private int quantity;

        /**
         * derived from OrderLineItem.getLineItemSubTotal()
         */
        private BigDecimal lineItemSubTotal;

        /**
         * Total without discount
         */
        private TaxableAmount subTotal;

        private TaxableAmount discountedSubTotal;

        private OfferApplicableObject.AppliedOfferInfo appliedOfferInfo;

        private Date modifiedDate;

        private List<ProductSnapshot.ChildProductSnapshot> childProducts;
    }
}
