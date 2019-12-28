package io.nextpos.merchandising.data;

import io.nextpos.ordermanagement.data.TaxableAmount;

import java.math.BigDecimal;

class OfferDiscountUtils {

    public static BigDecimal calculateDiscount(TaxableAmount amountToDiscountOn, Offer.DiscountDetails discountDetails) {

        return calculateDiscount(amountToDiscountOn, discountDetails, BigDecimal.ZERO);
    }

    public static BigDecimal calculateDiscount(TaxableAmount amountToDiscountOn, Offer.DiscountDetails discountDetails, BigDecimal overrideDiscount) {

        BigDecimal discountedPrice = BigDecimal.ZERO;
        final BigDecimal discountValue = overrideDiscount.compareTo(BigDecimal.ZERO) != 0 ? overrideDiscount : discountDetails.getDiscountValue();
        BigDecimal amount = amountToDiscountOn.getAmount();

        switch (discountDetails.getDiscountType()) {
            case AMOUNT:
                discountedPrice = discountValue;
                break;
            case AMOUNT_OFF:
                discountedPrice = amount.subtract(discountValue);
                break;
            case PERCENT_OFF:
                final BigDecimal discount = amount.multiply(discountValue);
                discountedPrice = amount.subtract(discount);
                break;
        }

        return discountedPrice;

    }
}