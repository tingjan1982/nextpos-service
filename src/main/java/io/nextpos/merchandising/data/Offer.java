 package io.nextpos.merchandising.data;

import io.nextpos.client.data.Client;
import io.nextpos.shared.exception.ConfigurationException;
import io.nextpos.shared.model.BaseObject;
import io.nextpos.shared.model.ClientObject;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.EnumSet;

@Entity(name = "client_offer")
@Inheritance(strategy = InheritanceType.JOINED)
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public abstract class Offer extends BaseObject implements ClientObject {

    private static final Logger LOGGER = LoggerFactory.getLogger(Offer.class);

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

    @ManyToOne
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Client client;

    private String name;

    private TriggerType triggerType;

    @Embedded
    private DiscountDetails discountDetails;

    @Embedded
    private EffectiveDetails effectiveDetails;

    Offer(final Client client, final String name, final TriggerType triggerType, DiscountType discountType, BigDecimal discountValue) {
        this.client = client;
        this.name = name;
        this.triggerType = triggerType;

        if (!supportedDiscountType().contains(discountType)) {
            throw new ConfigurationException("Discount type is not supported by this class: " + this.getClass().getName());
        }

        this.discountDetails = new DiscountDetails(discountType, discountValue);
        this.effectiveDetails = new EffectiveDetails(false, null, null);
    }

    public void updateOfferEffectiveDetails(boolean active) {
        this.updateOfferEffectiveDetails(active, null, null);
    }

    public void updateOfferEffectiveDetails(boolean active, Date startDate, Date endDate) {
        effectiveDetails.setActive(active);
        effectiveDetails.setStartDate(startDate);
        effectiveDetails.setEndDate(endDate);
    }

    public boolean isZeroDiscount() {
        return this.discountDetails.getDiscountValue().compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isActive() {

        final Date now = new Date();
        LOGGER.debug("Offer effective details: {}, now={}", effectiveDetails, now);

        if (!effectiveDetails.isActive()) {
            return false;
        }

        if (effectiveDetails.getStartDate() != null && now.before(effectiveDetails.getStartDate())) {
            return false;
        }

        return effectiveDetails.getEndDate() == null || !now.after(effectiveDetails.getEndDate());
    }

    public abstract EnumSet<DiscountType> supportedDiscountType();

    public enum TriggerType {

        ALWAYS,
        MEMBER,
        // todo: implement at checkout offer to display in payment page mainly for order/product discount
        AT_CHECKOUT,

        /**
         * Support in the future.
         */
        COUPON
    }

    @Embeddable
    @Data
    @NoArgsConstructor(access = AccessLevel.PACKAGE)
    @AllArgsConstructor
    public static class DiscountDetails {

        private DiscountType discountType;

        private BigDecimal discountValue;
    }

    public enum DiscountType {
        AMOUNT, AMOUNT_OFF, PERCENT_OFF
    }

    @Embeddable
    @Data
    @NoArgsConstructor(access = AccessLevel.PACKAGE)
    @AllArgsConstructor
    public static class EffectiveDetails {

        private boolean active;

        private Date startDate;

        private Date endDate;
    }
}
