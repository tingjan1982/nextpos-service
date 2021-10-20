package io.nextpos.merchandising.web.model;

import io.nextpos.merchandising.data.Offer;
import io.nextpos.merchandising.data.OfferType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@RequiredArgsConstructor
public class OfferResponse {

    private final String offerId;

    private final OfferType offerType;

    private final String offerName;

    @Deprecated
    private final String displayName;

    private final Offer.TriggerType triggerType;

    private final Offer.DiscountType discountType;

    private final BigDecimal discountValue;

    private final boolean active;

    private final Date startDate;

    private final Date endDate;

    private ProductOfferDetails productOfferDetails;

    @Data
    @AllArgsConstructor
    public static class ProductOfferDetails {

        private boolean appliesToAllProducts;

        private Map<String, String> productIds;

        private Map<String, String> excludedProductIds;

        private Map<String, String> productLabelIds;

        private List<ProductOfferProduct> selectedProducts;

        private List<ProductOfferProduct> selectedExcludedProducts;
    }

    @Data
    @AllArgsConstructor
    public static class ProductOfferProduct {

        private String labelId;

        private String productId;

        private String name;
    }
}
