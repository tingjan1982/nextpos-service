package io.nextpos.client.web.model;

import io.nextpos.client.data.ClientSetting;
import io.nextpos.shared.model.validator.ValidAttribute;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateClientRequest {

    @NotBlank
    private String clientName;

    @NotBlank
    private String timezone;

    @ValidAttribute
    private Map<String, String> attributes;

    private Map<ClientSetting.SettingName, ClientSettingRequest> clientSettings;

    private List<String> paymentMethodIds = new ArrayList<>();
}
