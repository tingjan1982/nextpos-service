package io.nextpos.shared.config;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.settings.data.CountrySettings;
import io.nextpos.settings.service.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.oauth2.common.util.RandomValueStringGenerator;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.Currency;

/**
 * https://www.baeldung.com/running-setup-logic-on-startup-in-spring
 */
@Component
public class BootstrapConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapConfig.class);

    public static final String MASTER_CLIENT = "master-client";

    public static final String DEFAULT_COUNTRY_CODE = "TW";

    private final ClientService clientService;

    private final SettingsService settingsService;

    private final SettingsConfigurationProperties settingsProperties;

    @Autowired
    public BootstrapConfig(final ClientService clientService, final SettingsService settingsService, final SettingsConfigurationProperties settingsProperties) {
        this.clientService = clientService;
        this.settingsService = settingsService;
        this.settingsProperties = settingsProperties;
    }

    @PostConstruct
    public void bootstrap() {

        if (clientService.getDefaultClient() == null) {
            final Client defaultClient = new Client(MASTER_CLIENT, MASTER_CLIENT, "1qaz2wsx3edc", DEFAULT_COUNTRY_CODE);
            defaultClient.setRoles("MASTER");
            final Client client = clientService.createClient(defaultClient);

            final String secret = new RandomValueStringGenerator(32).generate();
            LOGGER.info("test secret: {}", secret);
            LOGGER.info("Created master client: {}", client);
        }

        if (settingsService.findCountrySettings(DEFAULT_COUNTRY_CODE).isEmpty()) {
            final CountrySettings defaultCountrySettings = new CountrySettings(DEFAULT_COUNTRY_CODE, BigDecimal.valueOf(0.05), Currency.getInstance("TWD"));
            settingsProperties.getCommonAttributes().forEach(defaultCountrySettings::addCommonAttribute);

            settingsService.createCountrySettings(defaultCountrySettings);

            LOGGER.info("Created default country settings: {}", defaultCountrySettings);
        }
    }

    @Bean
    @Lazy
    public Client defaultClient() {
        return clientService.getDefaultClient();
    }

    @Bean
    @Lazy
    public CountrySettings defaultCountrySettings() {
        return settingsService.getCountrySettings(DEFAULT_COUNTRY_CODE);
    }
}
