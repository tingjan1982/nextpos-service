package io.nextpos.ordermanagement.web.model;

import io.nextpos.merchandising.data.OfferApplicableObject;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLog;
import io.nextpos.shared.aspect.OrderLogChangeObject;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@ValidDiscount
public class DiscountRequest implements OrderLogChangeObject {

    private String offerId;

    private String orderDiscount;

    private BigDecimal discount = BigDecimal.ZERO;

    @Override
    public void populateOrderLogEntries(final Order orderBeforeChange, final Order orderAfterChange, final OrderLog orderLog) {

        final OfferApplicableObject.AppliedOfferInfo offerInfoBeforeChange = orderBeforeChange.getAppliedOfferInfo();
        final OfferApplicableObject.AppliedOfferInfo offerInfoAfterChange = orderAfterChange.getAppliedOfferInfo();

        String beforeOffer = offerInfoBeforeChange != null ? offerInfoBeforeChange.getOfferDisplayName() : "N/A";
        String afterOffer = offerInfoAfterChange != null ? offerInfoAfterChange.getOfferDisplayName() : "N/A";

        orderLog.addChangeOrderLogEntry("discount", beforeOffer, afterOffer);
    }
}
