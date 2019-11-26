package io.nextpos.client.web.model;

import io.nextpos.client.data.ClientSetting;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ClientSettingResponse {

    private String id;

    private ClientSetting.SettingName settingName;

    private String value;

    private ClientSetting.ValueType valueType;

    private boolean enabled;
}
