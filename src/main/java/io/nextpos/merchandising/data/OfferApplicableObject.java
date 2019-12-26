package io.nextpos.merchandising.data;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

public interface OfferApplicableObject {

    /**
     * Template method to delegate applyOffer logic to concrete class while
     * initializing and storing AppliedOfferInfo to instance of OfferApplicableObject.
     *
     * @param offer
     * @param computedDiscount
     */
    default void applyAndRecordOffer(Offer offer, BigDecimal computedDiscount) {

        applyOffer(computedDiscount);

        final AppliedOfferInfo appliedOfferInfo = new AppliedOfferInfo(offer);
        this.setAppliedOfferInfo(appliedOfferInfo);
    }

    void applyOffer(BigDecimal computedDiscount);

    void setAppliedOfferInfo(AppliedOfferInfo appliedOfferInfo);


    @Data
    @NoArgsConstructor
    class AppliedOfferInfo {

        private String offerId;

        private String offerName;

        private String offerType;

        private Offer.DiscountDetails discountDetails;

        public AppliedOfferInfo(Offer offer) {
            this.offerId = offer.getId();
            this.offerName = offer.getName();
            this.offerType = offer.getClass().getSimpleName();
            this.discountDetails = offer.getDiscountDetails();
        }


    }
}
