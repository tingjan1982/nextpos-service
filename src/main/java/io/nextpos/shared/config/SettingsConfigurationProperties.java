package io.nextpos.shared.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "settings")
@Data
@NoArgsConstructor
public class SettingsConfigurationProperties {

    private List<String> commonAttributes;
}
