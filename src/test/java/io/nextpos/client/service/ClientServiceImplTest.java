package io.nextpos.client.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

        client.addAttribute(Client.ClientAttributes.UBN.name(), "22640971");

        final Client createdClient = clientService.createClient(client);

        assertThat(createdClient.getId()).isNotNull();
        assertThat(createdClient.getMasterPassword()).startsWith("{bcrypt}");

        final Client retrievedClient = clientService.getClient(createdClient.getId()).orElseThrow();

        assertThat(retrievedClient.getAttributes()).hasSize(1);
    }

    @Test
    @WithMockUser("rain.io.app@gmail.com")
    void crudClientUser() {

        final String username = "user@nextpos.io";
        final ClientUser clientUser = new ClientUser(new ClientUser.ClientUserId(username, client.getUsername()), "admin", SecurityConfig.Role.ADMIN_ROLE);

        final ClientUser createdUser = clientService.createClientUser(clientUser);

        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getPassword()).startsWith("{bcrypt}");
        assertThat(createdUser.getId()).isEqualTo(clientUser.getId());

        final ClientUser loadedClientUser = clientService.loadClientUser(client, username);
        assertThat(loadedClientUser).isNotNull();

        final UserDetails userDetails = clientService.loadUserByUsername(username);
        assertThat(userDetails).isNotNull();

        assertThat(clientService.getClientUsers(client)).hasSize(1);

        createdUser.setNickname("tom");
        createdUser.setPassword("password");
        createdUser.setRoles(SecurityConfig.Role.USER_ROLE);

        final ClientUser updatedUser = clientService.saveClientUser(createdUser);

        assertThat(updatedUser.getNickname()).isEqualTo("tom");
        assertThat(updatedUser.getId()).isEqualTo(clientUser.getId());
        assertThat(updatedUser.getRoles()).contains(SecurityConfig.Role.USER_ROLE);

        clientService.deleteClientUser(client, username);

        assertThat(clientService.getClientUsers(client)).isEmpty();

        assertThatThrownBy(() -> clientService.loadUserByUsername(username)).isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void compareClient() {

        final Client dummyClient = DummyObjects.dummyClient();
        clientService.createClient(dummyClient);

        final Client retrievedDummyClient = clientService.getClient(dummyClient.getId()).orElseThrow();

        assertThat(dummyClient).isEqualTo(retrievedDummyClient);
    }
}