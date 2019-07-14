package io.nextpos.settings.service;

import io.nextpos.settings.data.CountrySettings;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class SettingsServiceImplTest {

    @Autowired
    private SettingsService settingsService;


    @Test
    void createCountrySettings() {

        final CountrySettings countrySettings = new CountrySettings("US", BigDecimal.valueOf(0.08), Currency.getInstance("USD"));

        settingsService.createCountrySettings(countrySettings);

        final CountrySettings retrievedCountrySettings = settingsService.getCountrySettings(countrySettings.getIsoCountryCode());

        assertThat(retrievedCountrySettings).isNotNull();
    }
}