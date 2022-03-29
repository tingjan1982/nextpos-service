package io.nextpos.shared;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.client.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PrepareTestUtils {

    private final ClientService clientService;

    @Autowired
    public PrepareTestUtils(ClientService clientService) {
        this.clientService = clientService;
    }

    public Client createTestClient() {

        Client client = DummyObjects.dummyClient();
        clientService.saveClient(client);

        ClientUser clientUser = DummyObjects.dummyClientUser(client);
        clientService.saveClientUser(clientUser);

        return client;
    }
}
