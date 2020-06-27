package io.nextpos.shared.config;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.merchandising.data.Offer;
import io.nextpos.merchandising.data.OrderLevelOffer;
import io.nextpos.merchandising.data.ProductLevelOffer;
import io.nextpos.settings.data.CountrySettings;
import io.nextpos.settings.service.SettingsService;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.oauth2.common.util.RandomValueStringGenerator;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * https://www.baeldung.com/running-setup-logic-on-startup-in-spring
 */
@Component
public class BootstrapConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapConfig.class);

    public static final String MASTER_CLIENT = "master-client";

    public static final String DEFAULT_COUNTRY_CODE = "TW";

    public static final String DEFAULT_TIME_ZONE = "Asia/Taipei";

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

        Client defaultClient = clientService.getDefaultClient();

        if (defaultClient == null) {
            defaultClient = new Client(MASTER_CLIENT, MASTER_CLIENT, "1qaz2wsx3edc", DEFAULT_COUNTRY_CODE, DEFAULT_TIME_ZONE);
            defaultClient.setRoles(SecurityConfig.Role.MASTER_ROLE);
            final Client client = clientService.createClient(defaultClient);

            final String secret = new RandomValueStringGenerator(32).generate();
            LOGGER.info("test secret: {}", secret);
            LOGGER.info("Created master client: {}", client);
        }

        settingsService.findCountrySettings(DEFAULT_COUNTRY_CODE).ifPresentOrElse(settings -> {
            settings.setDecimalPlaces(0);
            settings.setRoundingMode(RoundingMode.HALF_UP);

            LOGGER.info("Updating default country settings: {}", settings);
            settingsService.saveCountrySettings(settings);

        }, () -> {
            final CountrySettings defaultCountrySettings = new CountrySettings(DEFAULT_COUNTRY_CODE,
                    BigDecimal.valueOf(0.05),
                    Currency.getInstance("TWD"),
                    0,
                    RoundingMode.HALF_UP);

            settingsProperties.getCommonAttributes().forEach(defaultCountrySettings::addCommonAttribute);

            LOGGER.info("Creating default country settings: {}", defaultCountrySettings);
            settingsService.saveCountrySettings(defaultCountrySettings);
        });
    }

    @Bean
    @Lazy
    public Map<OrderLevelOffer.GlobalOrderDiscount, OrderLevelOffer> globalOrderLevelOffers(Client defaultClient) {

        return Arrays.stream(OrderLevelOffer.GlobalOrderDiscount.values())
                .map(d -> {
                    final OrderLevelOffer orderLevelOffer = new OrderLevelOffer(defaultClient,
                            d.getDiscountName(),
                            Offer.TriggerType.AT_CHECKOUT,
                            d.getDiscountType(),
                            d.getDiscount());
                    orderLevelOffer.setId(d.name());
                    return Pair.of(d, orderLevelOffer);
                })
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight, (existing, replacement) -> existing, LinkedHashMap::new));
    }

    @Bean
    @Lazy
    public Map<ProductLevelOffer.GlobalProductDiscount, ProductLevelOffer> globalProductLevelOffers(Client defaultClient) {

        return Arrays.stream(ProductLevelOffer.GlobalProductDiscount.values())
                .map(d -> {
                    final ProductLevelOffer productLevelOffer = new ProductLevelOffer(defaultClient,
                            d.getDiscountName(),
                            Offer.TriggerType.AT_CHECKOUT,
                            d.getDiscountType(),
                            d.getDiscount(),
                            true);
                    productLevelOffer.setId(d.name());
                    return Pair.of(d, productLevelOffer);
                })
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight, (existing, replacement) -> existing, LinkedHashMap::new));
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
