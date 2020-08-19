package io.nextpos.product.web.model;

import io.nextpos.product.data.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private String id;

    private String versionId;

    private String name;

    private String internalName;

    private Version version;

    private String sku;

    private String description;

    private BigDecimal price;

    private BigDecimal costPrice;

    private String productLabelId;

    private String productLabel;

    // todo: consolidate working area id and product option ids in product and product label.
    private String workingAreaId;

    private List<String> productOptionIds;

    private List<ProductOptionResponse> productOptions;

    private boolean pinned;
}
