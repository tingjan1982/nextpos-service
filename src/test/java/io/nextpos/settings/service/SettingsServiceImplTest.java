package io.nextpos.settings.service;

import io.nextpos.settings.data.CountrySettings;
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
    void createCountrySettings() {

        final CountrySettings countrySettings = new CountrySettings("US", BigDecimal.valueOf(0.08), false, Currency.getInstance("USD"), 2, RoundingMode.HALF_UP);
        countrySettings.addCommonAttribute("UBN");

        settingsService.saveCountrySettings(countrySettings);

        final CountrySettings retrievedCountrySettings = settingsService.getCountrySettings(countrySettings.getIsoCountryCode());

        assertThat(retrievedCountrySettings).isNotNull();
        assertThat(retrievedCountrySettings.getCommonAttributes()).hasSize(1);
    }

    @Test
    void checkDecimalPlaceAndRoundingBehavior() {

        final CountrySettings taiwanSettings = new CountrySettings("TW",
                BigDecimal.valueOf(0.05),
                true,
                Currency.getInstance("TWD"),
                0,
                RoundingMode.HALF_UP);

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