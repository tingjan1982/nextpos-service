package io.nextpos.ordermanagement.web.model;

import io.nextpos.merchandising.data.OfferApplicableObject;
import io.nextpos.merchandising.data.ProductLevelOffer;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.ordermanagement.data.OrderLog;
import io.nextpos.ordermanagement.data.ProductSnapshot;
import io.nextpos.shared.aspect.OrderLogChangeObject;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;

import javax.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class UpdateOrderLineItemRequest implements OrderLogChangeObject {

    /**
     * This is not required to pass in via the API, but is set for creating OrderLog in OrderLogAspect.
     */
    private String lineItemId;

    @PositiveOrZero
    private int quantity;

    private List<OrderProductOptionRequest> productOptions;

    private BigDecimal overridePrice = BigDecimal.ZERO;

    private ProductLevelOffer.GlobalProductDiscount productDiscount;

    private BigDecimal discountValue;

    public List<ProductSnapshot.ProductOptionSnapshot> toProductOptionSnapshots() {

        if (CollectionUtils.isEmpty(productOptions)) {
            return Collections.emptyList();
        }

        return productOptions.stream()
                .map(p -> new ProductSnapshot.ProductOptionSnapshot(p.getOptionName(), p.getOptionValue(), p.getOptionPrice()))
                .collect(Collectors.toList());
    }

    @Override
    public void populateOrderLogEntries(final Order orderBeforeChange, final Order orderAfterChange, final OrderLog orderLog) {

        final OrderLineItem lineItemBeforeChange = orderBeforeChange.getOrderLineItem(lineItemId);
        final OrderLineItem lineItemAfterChange = orderAfterChange.getOrderLineItem(lineItemId);

        orderLog.addChangeOrderLogEntry("quantity", String.valueOf(lineItemBeforeChange.getQuantity()), String.valueOf(lineItemAfterChange.getQuantity()));

        final OfferApplicableObject.AppliedOfferInfo offerInfoBeforeChange = orderBeforeChange.getAppliedOfferInfo();
        final OfferApplicableObject.AppliedOfferInfo offerInfoAfterChange = orderAfterChange.getAppliedOfferInfo();

        String beforeOffer = offerInfoBeforeChange != null ? offerInfoBeforeChange.getOfferDisplayName() : null;
        String afterOffer = offerInfoAfterChange != null ? offerInfoAfterChange.getOfferDisplayName() : null;

        orderLog.addChangeOrderLogEntry("discount", beforeOffer, afterOffer);
    }
}
