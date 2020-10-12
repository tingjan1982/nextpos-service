package io.nextpos.product.web.model;

import io.nextpos.product.data.ProductOptionVersion;
import io.nextpos.product.data.Version;
import io.nextpos.shared.web.model.SimpleObjectResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@RequiredArgsConstructor
public class ProductOptionResponse {

    private final String id;

    private final String versionId;

    private final String optionName;

    private final Version version;

    private final boolean required;

    private final boolean multipleChoice;

    private final ProductOptionVersion.OptionType optionType;

    private final List<ProductOptionValueModel> optionValues;

    private List<SimpleObjectResponse> usedByProducts;

    private List<SimpleObjectResponse> usedByProductLabels;


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
