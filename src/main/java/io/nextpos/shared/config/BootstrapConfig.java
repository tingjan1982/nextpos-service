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

    private final ClientService clientService;

    @Autowired
    public BootstrapConfig(final ClientService clientService) {
        this.clientService = clientService;
    }

    @PostConstruct
    public void bootstrap() {

        final Client defaultClient = new Client("test-client");
        final Client client = clientService.createClient(defaultClient);

        LOGGER.info("Created test client: {}", client);
    }
}
