package io.nextpos.shared.config;


import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
@Data
@NoArgsConstructor
public class ApplicationProperties {

    private String hostname;

    private boolean autoActivateClient;
}
