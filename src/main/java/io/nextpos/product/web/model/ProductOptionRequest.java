package io.nextpos.product.web.model;

import io.nextpos.product.data.ProductOptionVersion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductOptionRequest {

    private String optionName;

    private ProductOptionVersion.OptionType optionType;

    private List<ProductOptionValueModel> optionValues;

}
