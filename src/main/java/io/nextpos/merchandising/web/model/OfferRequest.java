package io.nextpos.merchandising.web.model;

import io.nextpos.merchandising.data.Offer;
import io.nextpos.merchandising.data.OfferType;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
public class OfferRequest {

    @NotBlank
    private String offerName;

    private OfferType offerType = OfferType.ORDER;

    private Offer.TriggerType triggerType = Offer.TriggerType.AT_CHECKOUT;

    @NotNull
    private Offer.DiscountType discountType;

    @Positive
    private BigDecimal discountValue;

    private Date startDate;

    private Date endDate;

    private boolean appliesToAllProducts = true;

    private List<String> productIds = new ArrayList<>();

    private List<String> excludedProductIds = new ArrayList<>();

    private List<String> productLabelIds = new ArrayList<>();
}
