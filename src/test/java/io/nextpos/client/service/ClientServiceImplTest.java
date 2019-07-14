package io.nextpos.client.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.shared.DummyObjects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@Transactional
class ClientServiceImplTest {

    @Autowired
    private ClientServiceImpl clientService;

    @Test
    void createAndGetClient() {

        final Client client = DummyObjects.dummyClient();
        final Client createdClient = clientService.createClient(client);

        assertThat(createdClient.getId()).isNotNull();
        assertThat(createdClient.getMasterPassword()).startsWith("{bcrypt}");

        final Optional<Client> retrievedClient = clientService.getClient(createdClient.getId());

        assertThat(retrievedClient.isPresent()).isTrue();
    }

    @Test
    void createAndGetClientUser() {

        final ClientUser clientUser = new ClientUser(new ClientUser.ClientUserId("admin", "client-id"), "admin", "ADMIN");

        final ClientUser createdUser = clientService.createClientUser(clientUser);

        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getPassword()).startsWith("{bcrypt}");
        assertThat(createdUser.getId()).isEqualTo(clientUser.getId());

        clientService.loadUserByUsername("admin");
    }
}