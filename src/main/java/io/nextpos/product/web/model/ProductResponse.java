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

    private Version version;

    private String sku;

    private String description;

    private BigDecimal price;

    private String productLabel;

    private String workingArea;

    private List<ProductOptionResponse> productOptions;
}
