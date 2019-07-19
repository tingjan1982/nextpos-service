package io.nextpos.shared.model.validator;

import io.micrometer.core.instrument.util.StringUtils;
import org.springframework.util.CollectionUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class AttributeValidator implements ConstraintValidator<ValidAttribute, Map<String, String>> {

    @Override
    public void initialize(final ValidAttribute constraintAnnotation) {

    }

    @Override
    public boolean isValid(final Map<String, String> value, final ConstraintValidatorContext context) {

        AtomicBoolean valid = new AtomicBoolean(true);

        if (!CollectionUtils.isEmpty(value)) {
            value.forEach((k, v) -> {
                if (StringUtils.isBlank(k) || StringUtils.isBlank(v)) {
                    final String errorMsg = String.format("[%s=%s] cannot be blank.", k, v);
                    context.buildConstraintViolationWithTemplate(errorMsg)
                            .addBeanNode()
                            .inContainer(Map.class, 1)
                            .inIterable().atKey(k)
                            .addConstraintViolation();

                    valid.set(false);
                }
            });

        }

        return valid.get();
    }
}
