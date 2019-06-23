package io.nextpos.shared.config;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * https://www.baeldung.com/running-setup-logic-on-startup-in-spring
 */
@Component
public class BootstrapConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapConfig.class);

    public static final String TEST_CLIENT = "test-client";

    private final ClientService clientService;

    @Autowired
    public BootstrapConfig(final ClientService clientService) {
        this.clientService = clientService;
    }

    @PostConstruct
    public void bootstrap() {

        if (clientService.getDefaultClient() == null) {
            final Client defaultClient = new Client(TEST_CLIENT, TEST_CLIENT, "secret");
            final Client client = clientService.createClient(defaultClient);

            LOGGER.info("Created test client: {}", client);
        }
    }
}
