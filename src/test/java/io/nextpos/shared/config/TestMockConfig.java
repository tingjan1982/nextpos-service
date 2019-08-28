package io.nextpos.shared.config;

import io.nextpos.shared.auth.OAuth2Helper;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestMockConfig {

    @Bean
    @ConditionalOnProperty(name = "nomock", havingValue = "false", matchIfMissing = true)
    public OAuth2Helper oAuth2Helper() {
        final OAuth2Helper mock = Mockito.mock(OAuth2Helper.class);
        Mockito.when(mock.getCurrentPrincipal()).thenReturn("test-user");

        return mock;
    }
}
