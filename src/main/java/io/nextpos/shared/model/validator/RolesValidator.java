package io.nextpos.shared.model.validator;

import org.springframework.util.CollectionUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;

public class RolesValidator implements ConstraintValidator<ValidRoles, List<String>> {

    private static final List<String> VALID_ROLES = List.of("ADMIN", "USER");

    @Override
    public void initialize(final ValidRoles constraintAnnotation) {

    }

    @Override
    public boolean isValid(final List<String> value, final ConstraintValidatorContext context) {

        String errorMessage = null;

        if (CollectionUtils.isEmpty(value)) {
            errorMessage = "Roles cannot be empty";
        } else {

            final long validRoleCount = value.stream().filter(VALID_ROLES::contains).count();

            if (validRoleCount != value.size()) {
                errorMessage = "Not all roles are valid. Valid roles are: " + VALID_ROLES;
            }
        }

        if (errorMessage != null) {
            context.buildConstraintViolationWithTemplate(errorMessage).addConstraintViolation();
        }

        return errorMessage == null;
    }


}
