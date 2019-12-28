package io.nextpos.shared.config;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.ordermanagement.data.OrderSettings;
import io.nextpos.settings.data.CountrySettings;
import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.auth.OAuth2Helper;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;

@Configuration
public class TestMockConfig {

    @Bean
    @ConditionalOnProperty(name = "nomock", havingValue = "false", matchIfMissing = true)
    public OAuth2Helper oAuth2Helper() {
        final OAuth2Helper mock = Mockito.mock(OAuth2Helper.class);
        final ClientUser clientUser = DummyObjects.dummyClientUser();

        Mockito.when(mock.getCurrentPrincipal()).thenReturn(clientUser.getId().getUsername());
        Mockito.when(mock.resolveCurrentClientUser(any(Client.class))).thenReturn(clientUser);

        return mock;
    }

    @Bean
    public OrderSettings defaultOrderSettings(CountrySettings defaultCountrySettings) {
        return new OrderSettings(defaultCountrySettings.getTaxRate(), false, defaultCountrySettings.getCurrency(), BigDecimal.valueOf(0.1));
    }
}
