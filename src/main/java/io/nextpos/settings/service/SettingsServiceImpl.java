package io.nextpos.settings.service;

import io.nextpos.settings.data.CountrySettings;
import io.nextpos.settings.data.CountrySettingsRepository;
import io.nextpos.shared.exception.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Optional;

@Service
@Transactional
public class SettingsServiceImpl implements SettingsService {

    @Autowired
    private final CountrySettingsRepository countrySettingsRepository;

    public SettingsServiceImpl(final CountrySettingsRepository countrySettingsRepository) {
        this.countrySettingsRepository = countrySettingsRepository;
    }

    @Override
    public CountrySettings createCountrySettings(final CountrySettings countrySettings) {
        return countrySettingsRepository.save(countrySettings);
    }

    @Override
    public CountrySettings getCountrySettings(final String isoCountryCode) {
        return countrySettingsRepository.findById(isoCountryCode).orElseThrow(() -> {
            throw new ObjectNotFoundException(isoCountryCode, CountrySettings.class);
        });
    }

    @Override
    public Optional<CountrySettings> findCountrySettings(final String isoCountryCode) {
        return countrySettingsRepository.findById(isoCountryCode);
    }
}
