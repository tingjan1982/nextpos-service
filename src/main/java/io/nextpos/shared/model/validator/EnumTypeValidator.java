package io.nextpos.shared.model.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;

/**
 * Reference:
 * https://stackoverflow.com/questions/6768905/get-enum-instance-from-class-extends-enum-using-string-value
 */
public class EnumTypeValidator implements ConstraintValidator<ValidEnum, String> {

    private Class<? extends Enum> enumType;

    @Override
    public void initialize(final ValidEnum constraintAnnotation) {
        enumType = constraintAnnotation.enumType();
    }

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {

        try {
            Enum.valueOf(this.enumType, value);

            return true;
        } catch (Exception e) {
            final String errorMsg = "Invalid enum type. Valid values are " + Arrays.toString(this.enumType.getEnumConstants());
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(errorMsg).addConstraintViolation();
            return false;
        }
    }
}
