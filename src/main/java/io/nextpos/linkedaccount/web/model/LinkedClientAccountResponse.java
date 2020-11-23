package io.nextpos.linkedaccount.web.model;

import io.nextpos.client.data.Client;
import io.nextpos.linkedaccount.data.LinkedClientAccount;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class LinkedClientAccountResponse {

    private ClientAccount sourceClient;

    private List<ClientAccount> linkedClientAccounts;

    public LinkedClientAccountResponse(LinkedClientAccount linkedClientAccount) {

        sourceClient = new ClientAccount(linkedClientAccount.getSourceClient());
        linkedClientAccounts = linkedClientAccount.getLinkedClients().stream()
                .map(ClientAccount::new)
                .collect(Collectors.toList());
    }

    @Data
    public static class ClientAccount {

        private String id;

        private String clientName;

        private String username;

        public ClientAccount(Client client) {

            id = client.getId();
            clientName = client.getClientName();
            username = client.getUsername();
        }
    }
}
