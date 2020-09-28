package io.nextpos.client.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ClientStatusServiceImplTest {

    private final ClientStatusService clientStatusService;

    private final ClientService clientService;

    @Autowired
    ClientStatusServiceImplTest(ClientStatusService clientStatusService, ClientService clientService) {
        this.clientStatusService = clientStatusService;
        this.clientService = clientService;
    }

    @Test
    void checkClientStatus() {

        Client client = new Client("Attic", "rain.io.app@gmail.com", "Secret1", "TW", "Asia/Taipei");
        clientService.saveClient(client);

        final ClientStatus clientStatus = clientStatusService.checkClientStatus(client);

        assertThat(clientStatus.getId()).isEqualTo(client.getId());
        assertThat(clientStatus.getClient()).isNotNull();
        assertThat(clientStatus.isNoTableLayout()).isTrue();
        assertThat(clientStatus.isNoTable()).isTrue();
        assertThat(clientStatus.isNoCategory()).isTrue();
        assertThat(clientStatus.isNoProduct()).isTrue();
        assertThat(clientStatus.isNoWorkingArea()).isTrue();
        assertThat(clientStatus.isNoPrinter()).isTrue();
        assertThat(clientStatus.isNoElectronicInvoice()).isTrue();
    }
}