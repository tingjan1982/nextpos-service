package io.nextpos.settings.service;

import io.nextpos.settings.data.CountrySettings;

import java.util.Optional;

public interface SettingsService {

    CountrySettings saveCountrySettings(CountrySettings countrySettings);

    CountrySettings getCountrySettings(String isoCountryCode);

    Optional<CountrySettings> findCountrySettings(String isoCountryCode);
}
