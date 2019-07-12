package io.nextpos.settings.service;

import io.nextpos.settings.data.CountrySettings;

public interface SettingsService {

    CountrySettings getCountrySettings(String isoCountryCode);
}
