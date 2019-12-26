package io.nextpos.merchandising.data;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

public interface OfferApplicableObject {

    default void applyAndRecordOffer(Offer offer, BigDecimal computedDiscount) {
        this.applyAndRecordOffer(offer, computedDiscount, BigDecimal.ZERO);
    }

    /**
     * Template method to delegate applyOffer logic to concrete class while
     * initializing and storing AppliedOfferInfo to instance of OfferApplicableObject.
     *
     * @param offer
     * @param computedDiscount
     */
    default void applyAndRecordOffer(Offer offer, BigDecimal computedDiscount, BigDecimal overrideDiscount) {

        applyOffer(computedDiscount);

        final AppliedOfferInfo appliedOfferInfo = new AppliedOfferInfo(offer, overrideDiscount);
        this.setAppliedOfferInfo(appliedOfferInfo);
    }

    default BigDecimal replayOfferIfExists(BigDecimal amountWithoutTaxToDiscountOn) {

        final AppliedOfferInfo appliedOfferInfo = getAppliedOfferInfo();

        if (appliedOfferInfo != null) {
            return OfferDiscountUtils.calculateDiscount(amountWithoutTaxToDiscountOn, appliedOfferInfo.getDiscountDetails(), appliedOfferInfo.getOverrideDiscount());
        }

        return BigDecimal.ZERO;
    }

    void applyOffer(BigDecimal computedDiscount);

    AppliedOfferInfo getAppliedOfferInfo();

    void setAppliedOfferInfo(AppliedOfferInfo appliedOfferInfo);


    @Data
    @NoArgsConstructor
    class AppliedOfferInfo {

        private String offerId;

        private String offerName;

        private String offerType;

        private Offer.DiscountDetails discountDetails;

        private BigDecimal overrideDiscount;

        public AppliedOfferInfo(Offer offer, BigDecimal overrideDiscount) {
            this.offerId = offer.getId();
            this.offerName = offer.getName();
            this.offerType = offer.getClass().getSimpleName();
            this.discountDetails = offer.getDiscountDetails();
            this.overrideDiscount = overrideDiscount;
        }
    }
}
