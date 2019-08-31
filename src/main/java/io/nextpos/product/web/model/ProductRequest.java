package io.nextpos.product.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    @NotBlank
    private String name;

    private String sku;

    private String description;

    @PositiveOrZero
    private BigDecimal price;

    private String productLabelId;

    private String workingAreaId;

    private List<String> productOptionIds;
}
