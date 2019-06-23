package io.nextpos.client.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientRepository;
import io.nextpos.shared.config.BootstrapConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Optional;

@Service
@Transactional
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;

    @Autowired
    public ClientServiceImpl(final ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Override
    public Client createClient(final Client client) {
        return clientRepository.save(client);
    }

    @Override
    public Optional<Client> getClient(final String clientId) {
        return clientRepository.findById(clientId);
    }

    @Override
    public Client getDefaultClient() {
        return clientRepository.findByClientName(BootstrapConfig.TEST_CLIENT);
    }
}
