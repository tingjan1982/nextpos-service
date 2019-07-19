package io.nextpos.product.web.model;

import io.nextpos.product.data.ProductOptionVersion;
import io.nextpos.product.data.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductOptionResponse {

    private String id;

    private String versionId;

    private String optionName;

    private Version version;

    private ProductOptionVersion.OptionType optionType;

    private List<ProductOptionValueModel> optionValues;
}
