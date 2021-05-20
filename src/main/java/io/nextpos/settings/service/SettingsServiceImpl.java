package io.nextpos.settings.service;

import io.nextpos.settings.data.CountrySettings;
import io.nextpos.settings.data.CountrySettingsRepository;
import io.nextpos.settings.data.PaymentMethod;
import io.nextpos.settings.data.PaymentMethodRepository;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.service.annotation.JpaTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@JpaTransaction
public class SettingsServiceImpl implements SettingsService {

    private final CountrySettingsRepository countrySettingsRepository;

    private final PaymentMethodRepository paymentMethodRepository;

    @Autowired
    public SettingsServiceImpl(final CountrySettingsRepository countrySettingsRepository, PaymentMethodRepository paymentMethodRepository) {
        this.countrySettingsRepository = countrySettingsRepository;
        this.paymentMethodRepository = paymentMethodRepository;
    }

    @Override
    public CountrySettings saveCountrySettings(final CountrySettings countrySettings) {
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

    @Override
    public PaymentMethod getOrCreatePaymentMethod(String paymentKey, String displayName, int ordering) {

        return paymentMethodRepository.findByPaymentKey(paymentKey).orElseGet(() -> {
            final PaymentMethod paymentMethod = new PaymentMethod(paymentKey, displayName, ordering);
            return paymentMethodRepository.save(paymentMethod);
        });
    }

    @Override
    public Optional<PaymentMethod> getPaymentMethod(String id) {
        return paymentMethodRepository.findById(id);
    }
}
