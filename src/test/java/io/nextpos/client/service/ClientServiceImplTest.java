package io.nextpos.client.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.shared.DummyObjects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;

import javax.transaction.Transactional;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ClientServiceImplTest {

    @Autowired
    private ClientServiceImpl clientService;

    private Client client;

    @BeforeEach
    void prepare() {
        client = DummyObjects.dummyClient();
    }

    @Test
    void createAndGetClient() {

        client.getAttributes().put(Client.ClientAttributes.UBN.name(), "22640971");

        final Client createdClient = clientService.createClient(client);

        assertThat(createdClient.getId()).isNotNull();
        assertThat(createdClient.getMasterPassword()).startsWith("{bcrypt}");

        final Optional<Client> retrievedClient = clientService.getClient(createdClient.getId());

        assertThat(retrievedClient.isPresent()).isTrue();
        assertThat(retrievedClient.orElseThrow().getAttributes()).hasSize(1);
    }

    @Test
    @WithMockUser("admin@nextpos.io")
    void createAndGetClientUser() {

        final String username = "user@nextpos.io";
        final ClientUser clientUser = new ClientUser(new ClientUser.ClientUserId(username, client.getUsername()), "admin", "ADMIN");

        final ClientUser createdUser = clientService.createClientUser(clientUser);

        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getPassword()).startsWith("{bcrypt}");
        assertThat(createdUser.getId()).isEqualTo(clientUser.getId());

        final UserDetails userDetails = clientService.loadUserByUsername(username);
        assertThat(userDetails).isNotNull();

        assertThat(clientService.getClientUsers(client)).hasSize(1);
    }

    @Test
    void compareClient() {

        final Client dummyClient = DummyObjects.dummyClient();
        clientService.createClient(dummyClient);

        final Client retrievedDummyClient = clientService.getClient(dummyClient.getId()).orElseThrow();

        assertThat(dummyClient).isEqualTo(retrievedDummyClient);
    }
}