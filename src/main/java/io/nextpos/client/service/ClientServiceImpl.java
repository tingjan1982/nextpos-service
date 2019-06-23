package io.nextpos.client.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientRepository;
import io.nextpos.shared.config.BootstrapConfig;
import io.nextpos.shared.config.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

@Service
@Transactional
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;

    private final JdbcClientDetailsService clientDetailsService;

    private final PasswordEncoder passwordEncoder;


    @Autowired
    public ClientServiceImpl(final ClientRepository clientRepository, final JdbcClientDetailsService clientDetailsService, final PasswordEncoder passwordEncoder) {
        this.clientRepository = clientRepository;
        this.clientDetailsService = clientDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Creates respective security related entries:
     * OAuth2 ClientDetails (client_credentials) - used for creating users
     * OAuth2 ClientDetails (password, refresh_token) - used for all other operations.
     *
     * @param client
     * @return
     */
    @Override
    public Client createClient(final Client client) {

        clientDetailsService.addClientDetails(toClientDetails(client));
        client.setMasterPassword(passwordEncoder.encode(client.getMasterPassword()));

        return clientRepository.save(client);
    }

    private ClientDetails toClientDetails(Client client) {

        BaseClientDetails result = new BaseClientDetails();
        result.setClientId(client.getUsername());
        result.setClientSecret(client.getMasterPassword());
        result.setAuthorizedGrantTypes(Arrays.asList("client_credentials", "password", "refresh_token"));
        result.setAccessTokenValiditySeconds(3600);
        result.setRefreshTokenValiditySeconds(3600);
        result.setScope(Arrays.asList("client:*", "user:*"));
        result.setResourceIds(Collections.singletonList(SecurityConfig.OAuthSettings.RESOURCE_ID));

        return result;
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
