package io.nextpos.client.service;

import io.nextpos.client.data.Client;
import io.nextpos.notification.data.NotificationDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@Transactional
class ClientActivationServiceImplTest {

    @Autowired
    private ClientActivationService clientActivationService;
    
    @Test
    void sendActivationNotification() throws Exception {

        final Client client = new Client("Ron", "tingjan1982@gmail.com", "Secret1", "TW");
        client.setId("TINGJAN1");

        final CompletableFuture<NotificationDetails> future = clientActivationService.sendActivationNotification(client);
        future.get(10, TimeUnit.SECONDS);
    }
}