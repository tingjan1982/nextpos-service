package io.nextpos.product.web.model;

import io.nextpos.product.data.ProductType;
import io.nextpos.product.data.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@RequiredArgsConstructor
public class ProductResponse {

    private final String id;

    private final ProductType productType;

    private final String versionId;

    private final String name;

    private final String internalName;

    private final Version version;

    private final String sku;

    private final String description;

    private final BigDecimal price;

    private final BigDecimal costPrice;

    private final String productLabelId;

    private final String productLabel;

    // todo: consolidate working area id and product option ids in product and product label.
    private final String workingAreaId;

    private final List<String> productOptionIds;

    private final List<ProductOptionResponse> productOptions;

    private final boolean pinned;

    private List<ChildProduct> childProducts;

    @Data
    @AllArgsConstructor
    public static class ChildProduct {

        private String id;

        private String name;

        private String internalName;
    }

}
