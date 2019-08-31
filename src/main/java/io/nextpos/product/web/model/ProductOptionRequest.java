package io.nextpos.product.web.model;

import io.nextpos.product.data.ProductOptionVersion;
import io.nextpos.shared.model.validator.ValidEnum;
import io.nextpos.shared.model.validator.ValidOptionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ValidOptionType
public class ProductOptionRequest {

    @NotBlank
    private String optionName;

    @ValidEnum(enumType = ProductOptionVersion.OptionType.class)
    private String optionType;

    private boolean required;

    @Valid
    private List<ProductOptionValueModel> optionValues;

}
