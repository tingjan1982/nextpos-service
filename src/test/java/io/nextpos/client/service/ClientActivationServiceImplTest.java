package io.nextpos.client.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.notification.data.NotificationDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import javax.transaction.Transactional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@TestPropertySource(properties = "app.hostname=dummyhost")
class ClientActivationServiceImplTest {

    @Autowired
    private ClientActivationService clientActivationService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Client client;


    @BeforeEach
    void prepare() {
        client = new Client("Ron", "tingjan1982@gmail.com", "Secret1", "TW", "Asia/Taipei");
        clientService.createClient(client);
    }

    @Test
    void sendActivationNotification() throws Exception {

        final CompletableFuture<NotificationDetails> future = clientActivationService.sendActivationNotification(client);
        future.get(10, TimeUnit.SECONDS);
    }

    @Test
    void resetClientPassword() throws Exception {

        final CompletableFuture<NotificationDetails> future = clientActivationService.sendResetPasscode(client.getUsername());
        future.get(10, TimeUnit.SECONDS);

        final String passcode = client.getAttribute(Client.ClientAttributes.PASSCODE.name());
        assertThat(passcode).isNotNull();

        assertThat(clientActivationService.verifyResetPasscode(client.getUsername(), passcode)).isTrue();

        assertThat(client.getAttribute(Client.ClientAttributes.PASSCODE_VERIFIED.name())).isNotNull();

        final String passwordToUpdate = "Secret2";
        final ClientUser updatedClientUser = clientActivationService.resetClientPassword(client.getUsername(), passwordToUpdate);

        assertThat(passwordEncoder.matches(passwordToUpdate, updatedClientUser.getPassword())).isTrue();
    }

    @Test
    void resolveHostName() throws Exception {

        final String hostName = ((ClientActivationServiceImpl) clientActivationService).resolveHostName();

        assertThat(hostName).isNotNull();
        assertThat(hostName).isEqualTo("dummyhost");
    }
}