package io.nextpos.client.web.model;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientSetting;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientResponse {

    private String id;

    private String clientName;

    private String username;

    private String encryptedPassword;

    private String country;

    private String timezone;

    private Client.Status status;

    private Map<String, String> attributes;

    private Map<ClientSetting.SettingName, ClientSettingResponse> clientSettings;
}
