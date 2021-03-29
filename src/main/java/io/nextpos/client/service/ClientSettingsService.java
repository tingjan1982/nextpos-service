package io.nextpos.client.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientSetting;

import java.util.List;
import java.util.Optional;

public interface ClientSettingsService {

    ClientSetting saveClientSettings(ClientSetting clientSetting);

    List<ClientSetting> getClientSettings(Client client);

    Optional<ClientSetting> getClientSettingByName(Client client, ClientSetting.SettingName settingName);

    ClientSetting getClientSettingByNameOrThrows(Client client, ClientSetting.SettingName settingName);

    boolean getClientSettingBooleanValue(Client client, ClientSetting.SettingName settingName);

    <T> T getActualStoredValue(ClientSetting clientSetting, Class<T> targetType);
}
