package io.nextpos.settings.service;

import io.nextpos.settings.data.CountrySettings;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;

@Service
@Transactional
public class SettingsServiceImpl implements SettingsService {


    @Override
    public CountrySettings getCountrySettings(final String isoCountryCode) {

        if (isoCountryCode.equals("TW")) {
            return new CountrySettings(isoCountryCode, BigDecimal.valueOf(0.05));
        }

        return null;
    }
}
