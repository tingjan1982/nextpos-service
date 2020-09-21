package io.nextpos.ordermanagement.web.model;

import io.nextpos.merchandising.data.OfferApplicableObject;
import io.nextpos.ordermanagement.data.OrderLog;

class OrderLogProvider {

    static OrderLog.OrderLogEntry appliedOfferInfoLog(OfferApplicableObject.AppliedOfferInfo from, OfferApplicableObject.AppliedOfferInfo to) {

        String beforeOffer = from != null ? from.getOfferDisplayName() : "-";
        String afterOffer = to != null ? to.getOfferDisplayName() : "-";

        return new OrderLog.OrderLogEntry("discount", beforeOffer, afterOffer);
    }
}
