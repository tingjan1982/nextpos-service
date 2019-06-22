package io.nextpos.client.service;

import io.nextpos.client.data.Client;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@Transactional
class ClientServiceImplTest {

    @Autowired
    private ClientService clientService;

    @Test
    void createAndGetClient() {

        final Client client = new Client("Ron");

        final Client createdClient = clientService.createClient(client);

        assertThat(createdClient.getId()).isNotNull();

        final Client retrievedClient = clientService.getClient(createdClient.getId());

        assertThat(retrievedClient).isNotNull();
    }
}