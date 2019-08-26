package io.nextpos.shared.model.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;

public class IpAddressValidator implements ConstraintValidator<ValidIpAddress, String> {

    @Override
    public void initialize(final ValidIpAddress constraintAnnotation) {

    }

    @Override
    public boolean isValid(final String ip, final ConstraintValidatorContext context) {

        String[] groups = ip.split("\\.");

        if (groups.length != 4)
            return false;

        try {
            return Arrays.stream(groups)
                    .map(Integer::parseInt)
                    .filter(i -> (i >= 0 && i <= 255))
                    .count() == 4;

        } catch(NumberFormatException e) {
            return false;
        }
    }
}
