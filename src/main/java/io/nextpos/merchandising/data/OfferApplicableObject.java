package io.nextpos.merchandising.data;

import io.nextpos.ordermanagement.data.TaxableAmount;
import lombok.AllArgsConstructor;
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

    default void removeOffer() {

        applyOffer(BigDecimal.ZERO);
        this.setAppliedOfferInfo(null);
    }

    default BigDecimal replayOfferIfExists(TaxableAmount amountToDiscountOn) {

        final AppliedOfferInfo appliedOfferInfo = getAppliedOfferInfo();

        if (appliedOfferInfo != null) {
            return OfferDiscountUtils.calculateDiscount(amountToDiscountOn, appliedOfferInfo.getDiscountDetails(), appliedOfferInfo.getOverrideDiscount());
        }

        return BigDecimal.ZERO;
    }

    void applyOffer(BigDecimal computedDiscount);

    AppliedOfferInfo getAppliedOfferInfo();

    void setAppliedOfferInfo(AppliedOfferInfo appliedOfferInfo);


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
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

        public String getOfferDisplayName() {
            BigDecimal discountValue = overrideDiscount != null && overrideDiscount.compareTo(BigDecimal.ZERO) > 0 ? overrideDiscount : discountDetails.getDiscountValue();
            String discountType = discountDetails.getDiscountType() == Offer.DiscountType.PERCENT_OFF ? "%" : "$";

            return String.format("%s - (%s)", offerName, discountType, discountValue);
        }

        public AppliedOfferInfo copy() {
            return new AppliedOfferInfo(offerId, offerName, offerType, discountDetails, overrideDiscount);
        }
    }
}
