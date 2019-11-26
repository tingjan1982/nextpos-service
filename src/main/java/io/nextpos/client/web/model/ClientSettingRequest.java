package io.nextpos.client.web.model;

import io.nextpos.client.data.ClientSetting;
import io.nextpos.shared.model.validator.ValidEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientSettingRequest {

    @ValidEnum(enumType = ClientSetting.SettingName.class)
    private String settingName;

    private String value;

    private boolean enabled;
}
