package io.nextpos.ordermanagement.web.model;

import com.google.common.collect.Iterables;
import io.nextpos.merchandising.data.ProductLevelOffer;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.ordermanagement.data.OrderLog;
import io.nextpos.shared.aspect.OrderLogChangeObject;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
public class OrderLineItemRequest implements OrderLogChangeObject {

    @NotBlank
    private String productId;

    @Positive
    private int quantity;

    private List<OrderProductOptionRequest> productOptions;

    private ProductLevelOffer.GlobalProductDiscount productDiscount;

    private BigDecimal discountValue;

    @Override
    public void populateOrderLogEntries(final Order orderBeforeChange, final Order orderAfterChange, final OrderLog orderLog) {

        final OrderLineItem last = Iterables.getLast(orderAfterChange.getOrderLineItems());
        orderLog.addOrderLogEntry("product", last.getProductSnapshot().getName());
        orderLog.addOrderLogEntry("quantity", String.valueOf(last.getQuantity()));
        orderLog.addOrderLogEntry("subtotal", last.getLineItemSubTotal().toString());

        if (last.getAppliedOfferInfo() != null) {
            orderLog.addOrderLogEntry("discount", last.getAppliedOfferInfo().getOfferDisplayName());
        }
    }
}
