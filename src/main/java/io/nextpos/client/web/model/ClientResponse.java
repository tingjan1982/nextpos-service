package io.nextpos.client.web.model;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientInfo;
import io.nextpos.client.data.ClientSetting;
import io.nextpos.subscription.data.ClientSubscriptionAccess;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;
import java.util.stream.Collectors;

@Data
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

    private ClientInfoResponse clientInfo;

    private ClientSubscriptionAccess clientSubscriptionAccess;


    public ClientResponse(Client client) {
        id = client.getId();
        clientName = client.getClientName();
        username = client.getUsername();
        encryptedPassword = client.getMasterPassword();
        country = client.getCountryCode();
        timezone = client.getTimezone();
        status = client.getStatus();
        attributes = client.getAttributes();

        clientSettings = client.getClientSettings().stream()
                .map(s -> new ClientSettingResponse(s.getId(), s.getName(), s.getStoredValue(), s.getValueType(), s.isEnabled()))
                .collect(Collectors.toMap(ClientSettingResponse::getSettingName, res -> res));
    }

    @Data
    public static class ClientInfoResponse {

        private final String ownerName;

        private final String contactNumber;

        private final String contactAddress;

        private final String operationStatus;

        private final String leadSource;

        private final String requirements;

        public ClientInfoResponse(ClientInfo clientInfo) {

            ownerName = clientInfo.getOwnerName();
            contactNumber = clientInfo.getContactNumber();
            contactAddress = clientInfo.getContactAddress();
            operationStatus = clientInfo.getOperationStatus();
            leadSource = clientInfo.getLeadSource();
            requirements = clientInfo.getRequirements();
        }
    }
}
