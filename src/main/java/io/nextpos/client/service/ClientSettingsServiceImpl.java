package io.nextpos.client.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientSetting;
import io.nextpos.client.data.ClientSettingsRepository;
import io.nextpos.shared.exception.GeneralApplicationException;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.service.annotation.JpaTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@JpaTransaction
public class ClientSettingsServiceImpl implements ClientSettingsService {

    private final ClientSettingsRepository clientSettingsRepository;

    private final ConversionService conversionService;

    @Autowired
    public ClientSettingsServiceImpl(final ClientSettingsRepository clientSettingsRepository, final ConversionService conversionService) {
        this.clientSettingsRepository = clientSettingsRepository;
        this.conversionService = conversionService;
    }

    @Override
    public ClientSetting saveClientSettings(final ClientSetting clientSetting) {
        return clientSettingsRepository.save(clientSetting);
    }

    @Override
    public List<ClientSetting> getClientSettings(final Client client) {
        return clientSettingsRepository.findAllByClient(client);
    }

    @Override
    public Optional<ClientSetting> getClientSettingByName(final Client client, final ClientSetting.SettingName settingName) {
        return clientSettingsRepository.findByClientAndName(client, settingName);
    }

    @Override
    public ClientSetting getClientSettingByNameOrThrows(final Client client, final ClientSetting.SettingName settingName) {
        return this.getClientSettingByName(client, settingName).orElseThrow(() -> {
            throw new ObjectNotFoundException(settingName.name(), ClientSetting.class);
        });
    }

    @Override
    public <T> T getActualStoredValue(ClientSetting clientSetting, Class<T> targetType) {

        final Class<?> intendedClassType = clientSetting.getValueType().getClassType();

        if (!intendedClassType.equals(targetType)) {
            throw new GeneralApplicationException("Specified target type is not intended for this ClientSetting. Eligible type: " + clientSetting.getValueType());
        }

        return conversionService.convert(clientSetting.getStoredValue(), targetType);
    }

}
