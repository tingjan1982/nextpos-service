package io.nextpos.client.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;

import java.util.Optional;

public interface ClientService {

    Client createClient(Client client);

    Optional<Client> getClient(final String clientId);

    Client getDefaultClient();

    void markClientAsDeleted(String clientId);

    ClientUser createClientUser(ClientUser clientUser);

}
