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
import io.nextpos.workingarea.data.WorkingArea;
import io.nextpos.workingarea.service.WorkingAreaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.provider.NoSuchClientException;
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class ClientServiceImplTest {

    @Autowired
    private ClientService clientService;

    @Autowired
    private JdbcClientDetailsService jdbcClientDetailsService;

    @Autowired
    private ClientPasswordRegistryRepository clientPasswordRegistryRepository;

    @Autowired
    private LinkedClientAccountService linkedClientAccountService;

    @Autowired
    private WorkingAreaService workingAreaService;

    private Client client;

    @BeforeEach
    void prepare() {
        client = DummyObjects.dummyClient();
    }

    @Test
    void createAndGetClient() {

        client.addAttribute(Client.ClientAttributes.UBN, "22640971");

        final Client createdClient = clientService.createClient(client);

        assertThat(createdClient.getId()).isNotNull();
        assertThat(createdClient.getUsername()).isNotNull();
        assertThat(createdClient.getMasterPassword()).startsWith("{bcrypt}");
        assertThatCode(() -> clientService.getClientUser(client, client.getUsername())).doesNotThrowAnyException();
        assertThat(jdbcClientDetailsService.loadClientByClientId(createdClient.getUsername())).isNotNull();

        assertThatCode(() -> clientService.authenticateClient(client.getUsername(), "secret")).doesNotThrowAnyException();

        final Client retrievedClient = clientService.getClient(createdClient.getId()).orElseThrow();

        assertThat(retrievedClient.getAttributes()).hasSize(1);

        final String newUsername = "tingjan1982@gmail.com";
        clientService.updateUsernameForClient(client, newUsername, "secret");

        assertThat(clientService.getClientOrThrows(client.getId())).satisfies(c -> {
            assertThat(c.getUsername()).isEqualTo(newUsername);
            assertThatCode(() -> clientService.getClientUser(c, newUsername)).doesNotThrowAnyException();
            assertThat(jdbcClientDetailsService.loadClientByClientId(newUsername)).isNotNull();
            assertThatThrownBy(() -> jdbcClientDetailsService.loadClientByClientId("rain.io.app@gmail.com")).isInstanceOf(NoSuchClientException.class);
        });

        clientService.deleteClient(client.getId());
    }

    @Test
    @WithMockUser("rain.io.app@gmail.com")
    void crudClientUser() {

        clientService.saveClient(client);

        final WorkingArea kitchen = new WorkingArea(client, "kitchen");
        workingAreaService.saveWorkingArea(kitchen);
        final WorkingArea bar = new WorkingArea(client, "bar");
        workingAreaService.saveWorkingArea(bar);

        final String username = "user@nextpos.io";
        final ClientUser clientUser = new ClientUser(client, username, "admin", SecurityConfig.Role.ADMIN_ROLE);
        clientUser.addWorkingArea(kitchen);
        clientUser.addWorkingArea(bar);

        final ClientUser createdUser = clientService.createClientUser(clientUser);

        assertThat(clientService.getClientUser(client, clientUser.getUsername())).satisfies(u -> {
            assertThat(u.getId()).isEqualTo(clientUser.getId());
            assertThat(u.getNickname()).isNotNull();
            assertThat(u.getPassword()).startsWith("{bcrypt}");
            assertThat(u.getRoles()).contains(SecurityConfig.Role.ADMIN_ROLE);
            assertThat(u.getWorkingAreas()).hasSize(2);
        });

        assertThat(workingAreaService.getWorkingArea(kitchen.getId()).getClientUsers()).hasSize(1);
        assertThat(workingAreaService.getWorkingArea(bar.getId()).getClientUsers()).hasSize(1);

        final ClientPasswordRegistry clientPasswordRegistry = clientPasswordRegistryRepository.findByClient(client).orElseThrow();
        assertThat(clientPasswordRegistry.isPasswordUsed("admin")).isTrue();

        assertThatThrownBy(() -> clientService.createClientUser(clientUser)).isInstanceOf(ObjectAlreadyExistsException.class);

        final ClientUser anotherUser = new ClientUser(client, "user2", "admin", SecurityConfig.Role.ADMIN_ROLE);

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

        clientService.saveClientUser(createdUser);

        assertThat(clientService.getClientUser(client, createdUser.getUsername())).satisfies(u -> {
            assertThat(u.getId()).isEqualTo(clientUser.getId());
            assertThat(u.getNickname()).isEqualTo("tom");
            assertThat(u.getRoles()).contains(SecurityConfig.Role.USER_ROLE);
        });

        assertThatThrownBy(() -> workingAreaService.deleteWorkingArea(kitchen)).isInstanceOf(BusinessLogicException.class);

        clientService.deleteClientUser(client, username);
        clientService.deleteClientUser(client, anotherUser.getUsername());

        assertThat(clientService.getClientUsers(client)).isEmpty();

        assertThatThrownBy(() -> clientService.loadUserByUsername(username)).isInstanceOf(UsernameNotFoundException.class);

        assertThat(workingAreaService.getWorkingArea(kitchen.getId()).getClientUsers()).isEmpty();
        assertThat(workingAreaService.getWorkingArea(bar.getId()).getClientUsers()).isEmpty();

        workingAreaService.deleteWorkingArea(kitchen);
        workingAreaService.deleteWorkingArea(bar);

        assertThat(workingAreaService.getWorkingAreas(client)).isEmpty();
    }

    @Test
    @WithMockUser("second@client")
    void getClientUsersFromSourceClient() {

        clientService.saveClient(client);
        final String username = "user@nextpos.io";
        final ClientUser clientUser = new ClientUser(client, username, "admin", SecurityConfig.Role.ADMIN_ROLE);
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