package io.nextpos.product.web.model;

import io.nextpos.product.data.ProductOptionVersion;
import io.nextpos.product.data.Version;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class ProductOptionResponse {

    private String id;

    private String versionId;

    private String optionName;

    private Version version;

    private boolean required;

    private boolean multipleChoice;

    private ProductOptionVersion.OptionType optionType;

    private List<ProductOptionValueModel> optionValues;


    public static ProductOptionResponse fromProductOptionVersion(ProductOptionVersion productOptionVersion) {

        final List<ProductOptionValueModel> optionValues = productOptionVersion.getOptionValues().stream()
                .map(pov -> new ProductOptionValueModel(pov.getOptionValue(), pov.getOptionPrice()))
                .collect(Collectors.toList());

        return new ProductOptionResponse(productOptionVersion.getProductOption().getId(),
                productOptionVersion.getId(),
                productOptionVersion.getOptionName(),
                productOptionVersion.getVersion(),
                productOptionVersion.isRequired(),
                productOptionVersion.getOptionType() == ProductOptionVersion.OptionType.MULTIPLE_CHOICE,
                productOptionVersion.getOptionType(),
                optionValues);
    }
}
