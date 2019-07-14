package io.nextpos.settings.service;

import io.nextpos.settings.data.CountrySettings;

import java.util.Optional;

public interface SettingsService {

    CountrySettings createCountrySettings(CountrySettings countrySettings);

    CountrySettings getCountrySettings(String isoCountryCode);

    Optional<CountrySettings> findCountrySettings(String isoCountryCode);
}
