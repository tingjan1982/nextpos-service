package io.nextpos.settings.web;

import io.nextpos.client.data.Client;
import io.nextpos.settings.data.CountrySettings;
import io.nextpos.settings.service.SettingsService;
import io.nextpos.settings.web.model.PaymentMethodsResponse;
import io.nextpos.shared.web.ClientResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/settings")
public class SettingsController {

    private final SettingsService settingsService;

    @Autowired
    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping("/paymentMethods")
    public PaymentMethodsResponse getPaymentMethods(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        final CountrySettings countrySettings = settingsService.getCountrySettings(client.getCountryCode());

        return new PaymentMethodsResponse(countrySettings.getSupportedPaymentMethods());
    }
}
