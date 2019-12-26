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
        return OfferDiscountUtils.calculateDiscount(objectToDiscount.getTotal().getAmountWithoutTax(), this.getDiscountDetails(), discountValue);
    }

    @Override
    public EnumSet<DiscountType> supportedDiscountType() {
        return EnumSet.of(DiscountType.AMOUNT_OFF, DiscountType.PERCENT_OFF);
    }


    public enum GlobalOrderDiscount {

        STAFF_DISCOUNT("Staff 20%", BigDecimal.valueOf(0.2)),
        VIP_DISCOUNT("VIP 15%", BigDecimal.valueOf(0.15)),
        ENTER_DISCOUNT("Enter Discount", BigDecimal.valueOf(0));

        private final String discountName;

        private final BigDecimal discount;

        GlobalOrderDiscount(final String discountName, final BigDecimal discount) {
            this.discountName = discountName;
            this.discount = discount;
        }

        public String getDiscountName() {
            return discountName;
        }

        public BigDecimal getDiscount() {
            return discount;
        }
    }
}
