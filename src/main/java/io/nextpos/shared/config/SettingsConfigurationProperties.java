package io.nextpos.shared.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * https://www.baeldung.com/configuration-properties-in-spring-boot
 */
@Component
@ConfigurationProperties(prefix = "settings")
@Data
@NoArgsConstructor
public class SettingsConfigurationProperties {

    private List<String> commonAttributes;

    /**
     * Key: country code
     * Value: list of country specific attributes.
     */
    private Map<String, List<String>> countryAttributes;
}
