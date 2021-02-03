package io.nextpos.ordermanagement.web.model;

import io.nextpos.membership.data.Membership;
import io.nextpos.membership.web.model.MembershipResponse;
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

    @Deprecated
    private final Order.TableInfo tableInfo;

    @Deprecated
    private final String tableDisplayName;

    private final List<Order.TableInfo> tables;

    private final String servedBy;

    private final Date createdDate;

    private final Date modifiedDate;

    private final Order.OrderState state;

    private final TaxableAmount total;

    private final TaxableAmount discountedTotal;

    private final BigDecimal discount;

    private final BigDecimal serviceCharge;

    private final boolean serviceChargeEnabled;

    private final BigDecimal orderTotal;

    private final BigDecimal orderTotalWithoutServiceCharge;

    private final Currency currency;

    private final List<OrderLineItemResponse> lineItems;

    private final Map<String, Object> metadata;

    private final List<OrderLog> orderLogs;

    private final Order.DemographicData demographicData;

    private final OfferApplicableObject.AppliedOfferInfo appliedOfferInfo;

    private final OrderDuration orderDuration;

    private OrderDuration orderPreparationTime;

    private List<OrderTransactionResponse> transactions;

    private MembershipResponse membership;


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
                            li.getAssociatedLineItemId(),
                            li.getCreatedDate(),
                            li.getModifiedDate(),
                            productSnapshot.getChildProducts());

                }).collect(Collectors.toList());

        final OrderSettings originalOrderSettings = (OrderSettings) order.getMetadata(Order.ORIGINAL_ORDER_SETTINGS);
        boolean serviceChargeEnabled = originalOrderSettings.getServiceCharge().compareTo(BigDecimal.ZERO) != 0;

        final OrderResponse response = new OrderResponse(order.getId(),
                order.getSerialId(),
                order.getOrderType(),
                order.getOneTableInfo(),
                order.getOneTableInfo().getDisplayName(),
                order.getTables(),
                order.getServedBy(),
                order.getCreatedDate(),
                order.getModifiedDate(),
                order.getState(),
                order.getTotal(),
                order.getDiscountedTotal(),
                order.getDiscount(),
                order.getServiceCharge(),
                serviceChargeEnabled,
                order.getOrderTotal(),
                order.getOrderTotalWithoutServiceCharge(),
                order.getCurrency(),
                orderLineItems,
                order.getMetadata(),
                order.getOrderLogs(),
                order.getDemographicData(),
                order.getAppliedOfferInfo(),
                order.getOrderDuration());

        final Membership membership = order.getMembership();

        if (membership != null) {
            response.setMembership(new MembershipResponse(membership));
        }

        return response;
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

        private String associatedLineItemId;

        private Date createdDate;

        private Date modifiedDate;

        private List<ProductSnapshot.ChildProductSnapshot> childProducts;
    }
}
