package io.nextpos.merchandising.data;

import java.math.BigDecimal;

public interface DiscountCalculator<T> {

    /**
     * Calculates the discounted amount relevant to objectToDiscount, which can be Order or OrderLineItem.
     *
     * @param objectToDiscount
     * @return
     */
    BigDecimal calculateDiscount(T objectToDiscount);

    BigDecimal calculateDiscount(T objectToDiscount, BigDecimal overrideDiscountValue);
}
