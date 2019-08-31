package io.nextpos.client.service;

import io.nextpos.client.data.ClientSetting;

public interface ClientSettingsService {

    ClientSetting saveClientSettings(ClientSetting clientSetting);

    ClientSetting getClientSettings(String id);

    <T> T getActualStoredValue(ClientSetting clientSetting, Class<T> targetType);
}
