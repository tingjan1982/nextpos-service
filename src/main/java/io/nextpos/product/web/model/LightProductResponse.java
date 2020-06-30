package io.nextpos.product.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LightProductResponse {

    private String id;

    private String name;

    private BigDecimal price;

    private String productLabelId;

    private boolean pinned;
}
