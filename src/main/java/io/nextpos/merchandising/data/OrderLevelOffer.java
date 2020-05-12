package io.nextpos.merchandising.data;

import io.nextpos.client.data.Client;
import io.nextpos.ordermanagement.data.Order;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import java.math.BigDecimal;
import java.util.EnumSet;

@Entity(name = "client_order_offer")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class OrderLevelOffer extends Offer implements DiscountCalculator<Order> {


    public OrderLevelOffer(final Client client, final String name, final TriggerType triggerType, final DiscountType discountType, final BigDecimal discountValue) {
        super(client, name, triggerType, discountType, discountValue);
    }

    @Override
    public BigDecimal calculateDiscount(final Order objectToDiscount) {

        final BigDecimal discountValue = this.getDiscountDetails().getDiscountValue();

        return this.calculateDiscount(objectToDiscount, discountValue);
    }

    @Override
    public BigDecimal calculateDiscount(final Order objectToDiscount, final BigDecimal discountValue) {
        return OfferDiscountUtils.calculateDiscount(objectToDiscount.getTotal(), this.getDiscountDetails(), discountValue);
    }

    @Override
    public EnumSet<DiscountType> supportedDiscountType() {
        return EnumSet.of(DiscountType.AMOUNT_OFF, DiscountType.PERCENT_OFF);
    }


    public enum GlobalOrderDiscount {

        NO_DISCOUNT("No Discount", DiscountType.PERCENT_OFF, BigDecimal.valueOf(-1)),
        VIP_DISCOUNT("VIP 15%", DiscountType.PERCENT_OFF, BigDecimal.valueOf(0.15)),
        STAFF_DISCOUNT("Staff 30%", DiscountType.PERCENT_OFF, BigDecimal.valueOf(0.3)),
        ENTER_DISCOUNT("Discount %", DiscountType.PERCENT_OFF, BigDecimal.valueOf(0)),
        DISCOUNT_AMOUNT_OFF("Discount $", DiscountType.AMOUNT_OFF, BigDecimal.valueOf(0));


        private final String discountName;

        private final DiscountType discountType;

        private final BigDecimal discount;

        GlobalOrderDiscount(final String discountName, final DiscountType discountType, final BigDecimal discount) {
            this.discountName = discountName;
            this.discountType = discountType;
            this.discount = discount;
        }

        public String getDiscountName() {
            return discountName;
        }

        public DiscountType getDiscountType() {
            return discountType;
        }

        public BigDecimal getDiscount() {
            return discount;
        }
    }
}
