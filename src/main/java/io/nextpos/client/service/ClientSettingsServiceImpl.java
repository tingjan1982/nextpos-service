package io.nextpos.client.service;

import io.nextpos.client.data.ClientSetting;
import io.nextpos.client.data.ClientSettingsRepository;
import io.nextpos.shared.exception.GeneralApplicationException;
import io.nextpos.shared.exception.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@Transactional
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
    public ClientSetting getClientSettings(final String id) {
        return clientSettingsRepository.findById(id).orElseThrow(() -> {
            throw new ObjectNotFoundException(id, ClientSetting.class);
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
