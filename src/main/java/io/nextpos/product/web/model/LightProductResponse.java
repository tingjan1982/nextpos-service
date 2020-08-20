package io.nextpos.product.web.model;

import io.nextpos.product.data.ProductType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class LightProductResponse {

    private String id;

    private ProductType productType;

    private String name;

    private BigDecimal price;

    private String productLabelId;

    private boolean pinned;
}
