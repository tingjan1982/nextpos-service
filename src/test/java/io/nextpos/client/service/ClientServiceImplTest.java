package io.nextpos.client.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientPasswordRegistry;
import io.nextpos.client.data.ClientPasswordRegistryRepository;
import io.nextpos.client.data.ClientUser;
import io.nextpos.linkedaccount.service.LinkedClientAccountService;
import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.config.SecurityConfig;
import io.nextpos.shared.exception.BusinessLogicException;
import io.nextpos.shared.exception.ObjectAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class ClientServiceImplTest {

    @Autowired
    private ClientServiceImpl clientService;

    @Autowired
    private ClientPasswordRegistryRepository clientPasswordRegistryRepository;

    @Autowired
    private LinkedClientAccountService linkedClientAccountService;

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

        assertThatCode(() -> clientService.authenticateClient(client.getUsername(), "secret")).doesNotThrowAnyException();

        final Client retrievedClient = clientService.getClient(createdClient.getId()).orElseThrow();

        assertThat(retrievedClient.getAttributes()).hasSize(1);
    }

    @Test
    @WithMockUser("rain.io.app@gmail.com")
    void crudClientUser() {

        clientService.saveClient(client);
        final String username = "user@nextpos.io";
        final ClientUser clientUser = new ClientUser(new ClientUser.ClientUserId(username, client.getUsername()), client,"admin", SecurityConfig.Role.ADMIN_ROLE);

        final ClientUser createdUser = clientService.createClientUser(clientUser);

        final ClientPasswordRegistry clientPasswordRegistry = clientPasswordRegistryRepository.findByClient(client).orElseThrow();
        assertThat(clientPasswordRegistry.isPasswordUsed("admin")).isTrue();

        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getPassword()).startsWith("{bcrypt}");
        assertThat(createdUser.getId()).isEqualTo(clientUser.getId());

        assertThatThrownBy(() -> clientService.createClientUser(clientUser)).isInstanceOf(ObjectAlreadyExistsException.class);

        final ClientUser anotherUser = new ClientUser(new ClientUser.ClientUserId("user2", client.getUsername()), client,"admin", SecurityConfig.Role.ADMIN_ROLE);

        assertThatThrownBy(() -> clientService.createClientUser(anotherUser)).isInstanceOf(BusinessLogicException.class);

        anotherUser.setPassword("anotherpassword");
        clientService.createClientUser(anotherUser);
        assertThat(clientService.getClientUsers(client)).hasSize(2);

        final ClientUser loadedClientUser = clientService.loadClientUser(client, username);
        assertThat(loadedClientUser).isNotNull();

        final UserDetails userDetails = clientService.loadUserByUsername(username);
        assertThat(userDetails).isNotNull();

        createdUser.setNickname("tom");
        createdUser.setPassword("password");
        createdUser.setRoles(SecurityConfig.Role.USER_ROLE);

        final ClientUser updatedUser = clientService.saveClientUser(createdUser);

        assertThat(updatedUser.getNickname()).isEqualTo("tom");
        assertThat(updatedUser.getId()).isEqualTo(clientUser.getId());
        assertThat(updatedUser.getRoles()).contains(SecurityConfig.Role.USER_ROLE);

        clientService.deleteClientUser(client, username);
        clientService.deleteClientUser(client, anotherUser.getId().getUsername());

        assertThat(clientService.getClientUsers(client)).isEmpty();

        assertThatThrownBy(() -> clientService.loadUserByUsername(username)).isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @WithMockUser("second@client")
    void getClientUsersFromSourceClient() {

        clientService.saveClient(client);
        final String username = "user@nextpos.io";
        final ClientUser clientUser = new ClientUser(new ClientUser.ClientUserId(username, client.getUsername()), client,"admin", SecurityConfig.Role.ADMIN_ROLE);
        clientService.createClientUser(clientUser);

        final Client secondClient = new Client("second client", "second@client", "1234", "TW", "Asia/Taipei");
        clientService.saveClient(secondClient);

        linkedClientAccountService.createLinkedClientAccount(client, secondClient);

        assertThat(clientService.getClientUsers(secondClient)).isNotEmpty();

        assertThat(clientService.loadUserByUsername(username)).isNotNull();
    }

    @Test
    void compareClient() {

        final Client dummyClient = DummyObjects.dummyClient();
        clientService.createClient(dummyClient);

        final Client retrievedDummyClient = clientService.getClient(dummyClient.getId()).orElseThrow();

        assertThat(dummyClient).isEqualTo(retrievedDummyClient);
    }
}