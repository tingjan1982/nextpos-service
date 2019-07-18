package io.nextpos.shared.model.validator;

import org.passay.*;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;

/**
 * https://www.baeldung.com/registration-password-strength-and-rules
 */
public class PasswordConstraintValidator implements ConstraintValidator<ValidPassword, String> {

    @Override
    public void initialize(final ValidPassword constraintAnnotation) {

    }

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {

        PasswordValidator validator = new PasswordValidator(Arrays.asList(
                new LengthRule(6, 30),
                new UppercaseCharacterRule(1),
                new DigitCharacterRule(1),
                new WhitespaceRule()));

        RuleResult result = validator.validate(new PasswordData(value));

        if (result.isValid()) {
            return true;
        }
        context.disableDefaultConstraintViolation();

        final String joinedMessage = String.join(" ", validator.getMessages(result));
        context.buildConstraintViolationWithTemplate(joinedMessage).addConstraintViolation();

        return false;
    }
}
