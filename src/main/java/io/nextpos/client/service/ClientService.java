package io.nextpos.client.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;
import java.util.Optional;

public interface ClientService extends UserDetailsService {

    Client createClient(Client client);

    Client saveClient(Client client);

    Client authenticateClient(String clientId, String password);

    void updateUsernameForClient(Client client, String username, String password);

    ClientUser updateClientUserPassword(Client client, ClientUser clientUser, String newPassword);

    void updateDefaultClientUserPassword(Client client, String newPassword);

    Optional<Client> getClient(String clientId);

    Client getClientOrThrows(String clientId);

    Optional<Client> getClientByStatuses(String clientId, Client.Status... status);

    Optional<Client> getClientByUsername(String username);

    Client getClientByUsernameOrThrows(String username);

    Client getDefaultClient();

    List<Client> getClients();

    void updateClientStatus(Client client, Client.Status status);

    void deleteClient(String id);

    ClientUser createClientUser(ClientUser clientUser);

    ClientUser getCurrentClientUser(Client client);

    ClientUser getClientUser(Client client, String username);

    ClientUser loadClientUser(Client client, String username);

    String getClientUsernameByPassword(Client client, String password);

    List<ClientUser> getClientUsers(Client client);

    ClientUser saveClientUser(ClientUser clientUser);

    void deleteClientUser(final Client client, String username);
}
