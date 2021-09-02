package io.nextpos.settings.service;

import io.nextpos.settings.data.CountrySettings;
import io.nextpos.settings.data.PaymentMethod;

import java.util.Optional;

public interface SettingsService {

    CountrySettings saveCountrySettings(CountrySettings countrySettings);

    CountrySettings getCountrySettings(String isoCountryCode);

    Optional<CountrySettings> findCountrySettings(String isoCountryCode);

    PaymentMethod getOrCreatePaymentMethod(String paymentName, String displayName, int ordering);

    Optional<PaymentMethod> getPaymentMethod(String id);

    PaymentMethod getPaymentMethodByPaymentKey(String paymentKey);
}
