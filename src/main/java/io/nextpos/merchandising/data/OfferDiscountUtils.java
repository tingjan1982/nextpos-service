package io.nextpos.merchandising.data;

import java.math.BigDecimal;

class OfferDiscountUtils {

    public static BigDecimal calculateDiscount(BigDecimal amountWithoutTaxToDiscountOn, Offer.DiscountDetails discountDetails) {

        return calculateDiscount(amountWithoutTaxToDiscountOn, discountDetails, BigDecimal.ZERO);
    }

    public static BigDecimal calculateDiscount(BigDecimal amountWithoutTaxToDiscountOn, Offer.DiscountDetails discountDetails, BigDecimal overrideDiscount) {

        BigDecimal discountedPrice = BigDecimal.ZERO;
        final BigDecimal discountValue = overrideDiscount.compareTo(BigDecimal.ZERO) != 0 ? overrideDiscount : discountDetails.getDiscountValue();

        switch (discountDetails.getDiscountType()) {
            case AMOUNT:
                discountedPrice = discountValue;
                break;
            case AMOUNT_OFF:
                discountedPrice = amountWithoutTaxToDiscountOn.subtract(discountValue);
                break;
            case PERCENT_OFF:
                final BigDecimal discount = amountWithoutTaxToDiscountOn.multiply(discountValue);
                discountedPrice = amountWithoutTaxToDiscountOn.subtract(discount);
                break;
        }

        return discountedPrice;

    }
}