package io.nextpos.client.web.model;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientInfo;
import io.nextpos.client.data.ClientSetting;
import io.nextpos.settings.web.model.PaymentMethodResponse;
import io.nextpos.subscription.data.ClientSubscriptionAccess;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class ClientResponse {

    private final String id;

    private final String clientName;

    private final String username;

    private final String encryptedPassword;

    private final String country;

    private final String timezone;

    private final Client.ClientType clientType;

    private final Client.Status status;

    private final Map<String, String> attributes;

    private final Map<ClientSetting.SettingName, ClientSettingResponse> clientSettings;

    private final List<PaymentMethodResponse> paymentMethods;

    private ClientInfoResponse clientInfo;

    private ClientSubscriptionAccess clientSubscriptionAccess;


    public ClientResponse(Client client) {
        id = client.getId();
        clientName = client.getClientName();
        username = client.getUsername();
        encryptedPassword = client.getMasterPassword();
        country = client.getCountryCode();
        timezone = client.getTimezone();
        clientType = client.getClientType();
        status = client.getStatus();
        attributes = client.getAttributes();

        clientSettings = client.getClientSettings().stream()
                .map(s -> new ClientSettingResponse(s.getId(), s.getName(), s.getStoredValue(), s.getValueType(), s.isEnabled()))
                .collect(Collectors.toMap(ClientSettingResponse::getSettingName, res -> res));

        paymentMethods = client.getSupportedPaymentMethods().stream()
                .map(PaymentMethodResponse::new)
                .collect(Collectors.toList());
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
