package io.nextpos.client.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;

import java.util.Optional;

public interface ClientService {

    Client createClient(Client client);

    Client saveClient(Client client);

    Optional<Client> getClient(final String clientId);

    Optional<Client> getClientByUsername(final String username);

    Client getDefaultClient();

    void markClientAsDeleted(String clientId);

    void deleteClient(String id);

    ClientUser createClientUser(ClientUser clientUser);
}
