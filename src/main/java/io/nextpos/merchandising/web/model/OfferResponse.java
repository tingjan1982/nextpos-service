package io.nextpos.merchandising.web.model;

import io.nextpos.merchandising.data.Offer;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class OfferResponse {

    private String offerId;

    private String offerName;

    @Deprecated
    private String displayName;

    private Offer.TriggerType triggerType;

    private Offer.DiscountType discountType;

    private BigDecimal discountValue;
}
