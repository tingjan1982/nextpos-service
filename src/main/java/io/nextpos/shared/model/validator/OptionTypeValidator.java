package io.nextpos.shared.model.validator;

import io.nextpos.product.data.ProductOptionVersion;
import io.nextpos.product.web.model.ProductOptionRequest;
import org.springframework.util.CollectionUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;

/**
 * Various spring validation use cases:
 * https://www.baeldung.com/javax-validation-method-constraints
 */
public class OptionTypeValidator implements ConstraintValidator<ValidOptionType, ProductOptionRequest> {

    @Override
    public void initialize(final ValidOptionType constraintAnnotation) {

    }

    @Override
    public boolean isValid(final ProductOptionRequest value, final ConstraintValidatorContext context) {

        try {
            final ProductOptionVersion.OptionType optionType = ProductOptionVersion.OptionType.valueOf(value.getOptionType());

            if (optionType == ProductOptionVersion.OptionType.FREE_TEXT) {
                if (!CollectionUtils.isEmpty(value.getOptionValues())) {
                    final String errorMsg = String.format("%s option type cannot have product options", optionType);
                    context.buildConstraintViolationWithTemplate(errorMsg)
                            .addPropertyNode("optionValues").addConstraintViolation();
                    return false;
                }
            } else {
                if (CollectionUtils.isEmpty(value.getOptionValues())) {
                    final String errorMsg = String.format("%s option type must have at least one product option", optionType);
                    context.buildConstraintViolationWithTemplate(errorMsg)
                            .addPropertyNode("optionValues").addConstraintViolation();
                    return false;
                }
            }
        } catch (Exception e) {
            final String errorMsg = "Invalid option type. Valid values are " + Arrays.toString(ProductOptionVersion.OptionType.values());
            context.buildConstraintViolationWithTemplate(errorMsg).addPropertyNode("optionType").addConstraintViolation();
            return false;
        }

        return true;
    }
}
