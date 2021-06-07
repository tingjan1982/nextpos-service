package io.nextpos.settings.service;

import io.nextpos.settings.data.CountrySettings;
import io.nextpos.settings.data.PaymentMethod;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class SettingsServiceImplTest {

    @Autowired
    private SettingsService settingsService;


    @Test
    void createCountrySettingsAndPaymentMethods() {

        final CountrySettings countrySettings = new CountrySettings("US", BigDecimal.valueOf(0.08), false, Currency.getInstance("USD"), 2, RoundingMode.HALF_UP, "1");
        countrySettings.addCommonAttribute("UBN");

        settingsService.saveCountrySettings(countrySettings);

        final CountrySettings retrievedCountrySettings = settingsService.getCountrySettings(countrySettings.getIsoCountryCode());

        assertThat(retrievedCountrySettings.getIsoCountryCode()).isEqualTo("US");
        assertThat(retrievedCountrySettings.getCommonAttributes()).hasSize(1);
        assertThat(retrievedCountrySettings.getSupportedPaymentMethods()).isEmpty();

        final PaymentMethod paymentMethod = settingsService.getOrCreatePaymentMethod("CASH", "Cash", 1);

        assertThat(paymentMethod.getId()).isNotNull();
        assertThat(paymentMethod.getPaymentKey()).isEqualTo("CASH");
        assertThat(paymentMethod.getDisplayName()).isEqualTo("Cash");

        retrievedCountrySettings.addSupportedPaymentMethod(paymentMethod);
        settingsService.saveCountrySettings(retrievedCountrySettings);

        assertThat(settingsService.getCountrySettings(countrySettings.getIsoCountryCode()).getSupportedPaymentMethods()).hasSize(1);

        retrievedCountrySettings.clearPaymentMethods();
        settingsService.saveCountrySettings(retrievedCountrySettings);

        assertThat(settingsService.getCountrySettings(countrySettings.getIsoCountryCode()).getSupportedPaymentMethods()).isEmpty();

    }

    @Test
    void checkDecimalPlaceAndRoundingBehavior() {

        final CountrySettings taiwanSettings = new CountrySettings("TW",
                BigDecimal.valueOf(0.05),
                true,
                Currency.getInstance("TWD"),
                0,
                RoundingMode.HALF_UP,
                "886");

        settingsService.saveCountrySettings(taiwanSettings);

        assertThat(new BigDecimal("4.01").setScale(taiwanSettings.getDecimalPlaces(), taiwanSettings.getRoundingMode())).isEqualTo("4");

        assertThat(new BigDecimal("4.1").setScale(taiwanSettings.getDecimalPlaces(), taiwanSettings.getRoundingMode())).isEqualTo("4");

        assertThat(new BigDecimal("4.4").setScale(taiwanSettings.getDecimalPlaces(), taiwanSettings.getRoundingMode())).isEqualTo("4");

        assertThat(new BigDecimal("4.49").setScale(taiwanSettings.getDecimalPlaces(), taiwanSettings.getRoundingMode())).isEqualTo("4");

        assertThat(new BigDecimal("4.499").setScale(taiwanSettings.getDecimalPlaces(), taiwanSettings.getRoundingMode())).isEqualTo("4");

        assertThat(new BigDecimal("4.5").setScale(taiwanSettings.getDecimalPlaces(), taiwanSettings.getRoundingMode())).isEqualTo("5");

        assertThat(new BigDecimal("4.501").setScale(taiwanSettings.getDecimalPlaces(), taiwanSettings.getRoundingMode())).isEqualTo("5");

        assertThat(new BigDecimal("4.51").setScale(taiwanSettings.getDecimalPlaces(), taiwanSettings.getRoundingMode())).isEqualTo("5");

        assertThat(new BigDecimal("4.6").setScale(taiwanSettings.getDecimalPlaces(), taiwanSettings.getRoundingMode())).isEqualTo("5");

        assertThat(new BigDecimal("4.9").setScale(taiwanSettings.getDecimalPlaces(), taiwanSettings.getRoundingMode())).isEqualTo("5");
    }
}