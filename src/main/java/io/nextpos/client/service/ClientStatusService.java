package io.nextpos.client.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientStatus;

public interface ClientStatusService {

    ClientStatus checkClientStatus(Client client);
}
