package io.nextpos.client.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;

import java.util.List;
import java.util.Optional;

public interface ClientService {

    Client createClient(Client client);

    Client saveClient(Client client);

    ClientUser updateClientUserPassword(Client client, ClientUser clientUser, String newPassword);

    void updateDefaultClientUserPassword(Client client, String newPassword);

    Optional<Client> getClient(String clientId);

    Client getClientOrThrows(String clientId);

    Optional<Client> getClientByStatuses(String clientId, Client.Status... status);

    Optional<Client> getClientByUsername(String username);

    Client getDefaultClient();

    void updateClientStatus(Client client, Client.Status status);

    void deleteClient(String id);

    ClientUser createClientUser(ClientUser clientUser);

    ClientUser getClientUser(Client client, String username);

    ClientUser loadClientUser(Client client, String username);

    List<ClientUser> getClientUsers(Client client);

    ClientUser saveClientUser(ClientUser clientUser);

    void deleteClientUser(final Client client, String username);
}
