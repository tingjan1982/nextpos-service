package io.nextpos.shared.model.validator;

import io.micrometer.core.instrument.util.StringUtils;
import io.nextpos.settings.data.CountrySettings;
import io.nextpos.settings.service.SettingsService;
import io.nextpos.shared.config.BootstrapConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * https://stackoverflow.com/questions/13350537/inject-service-in-constraintvalidator-bean-validator-jsr-303-spring
 */
public class AttributeValidator implements ConstraintValidator<ValidAttribute, Map<String, String>> {

    private final SettingsService settingsService;

    @Autowired
    public AttributeValidator(final SettingsService settingsService) {
        this.settingsService = settingsService;
    }


    @Override
    public void initialize(final ValidAttribute constraintAnnotation) {

    }

    @Override
    public boolean isValid(final Map<String, String> value, final ConstraintValidatorContext context) {

        AtomicBoolean valid = new AtomicBoolean(true);

        if (!CollectionUtils.isEmpty(value)) {
            value.forEach((k, v) -> {
                if (StringUtils.isBlank(k)) {
                    final String errorMsg = String.format("[key=%s] cannot be blank.", k);
                    context.buildConstraintViolationWithTemplate(errorMsg)
                            .addBeanNode()
                            .inContainer(Map.class, 1)
                            .inIterable().atKey(k)
                            .addConstraintViolation();

                    valid.set(false);
                } else {
                    final CountrySettings countrySettings = settingsService.getCountrySettings(BootstrapConfig.DEFAULT_COUNTRY_CODE);
                    if (!countrySettings.getCommonAttributes().contains(k)) {
                        final String errorMsg = String.format("[%s] is not a defined attribute.", k);
                        context.buildConstraintViolationWithTemplate(errorMsg)
                                .addBeanNode()
                                .inContainer(Map.class, 1)
                                .inIterable().atKey(k)
                                .addConstraintViolation();

                        valid.set(false);
                    }
                }
            });
        }

        return valid.get();
    }
}
