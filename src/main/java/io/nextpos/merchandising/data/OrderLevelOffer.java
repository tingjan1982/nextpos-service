package io.nextpos.merchandising.data;

import io.nextpos.client.data.Client;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.TaxableAmount;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Entity;
import java.math.BigDecimal;
import java.util.EnumSet;

@Entity(name = "client_order_offer")
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderLevelOffer extends Offer implements DiscountCalculator<Order> {

    public OrderLevelOffer(final Client client, final String name, final TriggerType triggerType, final DiscountType discountType, final BigDecimal discountValue) {
        super(client, name, triggerType, discountType, discountValue);
    }

    @Override
    public BigDecimal calculateDiscount(final Order objectToDiscount) {

        BigDecimal discountedPrice = BigDecimal.ZERO;

        final BigDecimal discountValue = this.getDiscountDetails().getDiscountValue();
        TaxableAmount taxableTotal = objectToDiscount.getDiscountedTotal();

        if (taxableTotal == null) {
            taxableTotal = objectToDiscount.getTotal();
        }

        final BigDecimal amountToDiscountOn = taxableTotal.getAmountWithoutTax();

        switch (this.getDiscountDetails().getDiscountType()) {
            case AMOUNT_OFF:
                discountedPrice = amountToDiscountOn.subtract(discountValue);
                break;
            case PERCENT_OFF:
                final BigDecimal discount = amountToDiscountOn.multiply(discountValue);
                discountedPrice = amountToDiscountOn.subtract(discount);
                break;
        }

        return discountedPrice;
    }

    @Override
    public EnumSet<DiscountType> supportedDiscountType() {
        return EnumSet.of(DiscountType.AMOUNT_OFF, DiscountType.PERCENT_OFF);
    }
}
