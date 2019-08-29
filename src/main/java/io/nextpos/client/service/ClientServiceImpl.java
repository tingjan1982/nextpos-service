package io.nextpos.client.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientRepository;
import io.nextpos.client.data.ClientUser;
import io.nextpos.client.data.ClientUserRepository;
import io.nextpos.shared.config.BootstrapConfig;
import io.nextpos.shared.config.SecurityConfig;
import io.nextpos.shared.exception.ObjectAlreadyExistsException;
import io.nextpos.shared.exception.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.NoSuchClientException;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
public class ClientServiceImpl implements ClientService, UserDetailsService {

    private final ClientRepository clientRepository;

    private final ClientUserRepository clientUserRepository;

    private final JdbcClientDetailsService clientDetailsService;

    private final PasswordEncoder passwordEncoder;


    @Autowired
    public ClientServiceImpl(final ClientRepository clientRepository, final ClientUserRepository clientUserRepository, final JdbcClientDetailsService clientDetailsService, final PasswordEncoder passwordEncoder) {
        this.clientRepository = clientRepository;
        this.clientUserRepository = clientUserRepository;
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

        checkClientExists(client);

        final ClientDetails clientDetails = toClientDetails(client);
        clientDetailsService.addClientDetails(clientDetails);

        final String plainPassword = client.getMasterPassword();
        final String encryptedPassword = passwordEncoder.encode(plainPassword);
        client.setMasterPassword(encryptedPassword);

        final Client savedClient = clientRepository.save(client);

        final String clientRoles = clientDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.joining(","));
        // client's default user will use email as the username.
        final ClientUser defaultClientUser = new ClientUser(new ClientUser.ClientUserId(client.getUsername(), client.getUsername()), plainPassword, clientRoles);
        this.createClientUser(defaultClientUser);

        return savedClient;
    }

    private void checkClientExists(final Client client) {

        clientRepository.findByUsername(client.getUsername()).ifPresent(c -> {
            throw new ObjectAlreadyExistsException(c.getId(), Client.class);
        });
    }

    private ClientDetails toClientDetails(Client client) {

        BaseClientDetails result = new BaseClientDetails();
        result.setClientId(client.getUsername());
        result.setClientSecret(client.getMasterPassword());
        result.setAuthorizedGrantTypes(Arrays.asList("client_credentials", "password", "refresh_token"));
        final int validPeriod = Long.valueOf(TimeUnit.DAYS.toSeconds(1)).intValue();
        result.setAccessTokenValiditySeconds(validPeriod);
        result.setRefreshTokenValiditySeconds(validPeriod);
        result.setScope(SecurityConfig.OAuthScopes.SCOPES);

        String[] roles = Stream.of("ADMIN", "USER", client.getRoles()).filter(Objects::nonNull).toArray(String[]::new);
        result.setAuthorities(AuthorityUtils.createAuthorityList(roles));
        result.setResourceIds(Collections.singletonList(SecurityConfig.OAuthSettings.RESOURCE_ID));

        return result;
    }

    @Override
    public Client saveClient(final Client client) {
        return clientRepository.save(client);
    }

    @Override
    public Optional<Client> getClient(final String clientId) {
        return clientRepository.findByIdAndStatusIn(clientId, Client.Status.ACTIVE, Client.Status.PENDING_ACTIVE);
    }

    /**
     * In OAuth2, clientId is the username in Authorization header.
     * Client object is created with clientId and username.
     * ClientUser object is created with username and clientId, which really is Client's username.
     *
     * @param username
     * @return
     */
    @Override
    public Optional<Client> getClientByUsername(final String username) {
        return clientRepository.findByUsername(username);
    }

    @Override
    public Client getDefaultClient() {
        return clientRepository.findByUsername(BootstrapConfig.MASTER_CLIENT).orElseGet(() -> {
            /*
             * remove ClientDetails from clientDetailsService. This is needed for passing unit test suites
             * as multiple spring containers are initiated when they have different configurations. (e.g. contains @TestPropertySource).
             */
            try {
                clientDetailsService.removeClientDetails(BootstrapConfig.MASTER_CLIENT);
            } catch (NoSuchClientException e) {
                // ignored as it will not be found for the first time.
            }

            return null;
        });
    }

    @Override
    public void markClientAsDeleted(final String clientId) {
        clientRepository.findById(clientId).ifPresent(client -> client.setStatus(Client.Status.DELETED));
    }

    @Override
    public void deleteClient(final String id) {
        clientRepository.findById(id).ifPresent(client -> {
            clientDetailsService.removeClientDetails(client.getUsername());
            clientUserRepository.deleteClientUsersByClientId(client.getUsername());
            clientRepository.delete(client);
        });
    }

    @Override
    public ClientUser createClientUser(final ClientUser clientUser) {

        clientUserRepository.findById(clientUser.getId()).ifPresent(u -> {
            throw new ObjectAlreadyExistsException(clientUser.getId().toString(), ClientUser.class);
        });

        final String encryptedPassword = passwordEncoder.encode(clientUser.getPassword());
        clientUser.setPassword(encryptedPassword);

        return clientUserRepository.save(clientUser);
    }

    @Override
    public ClientUser getClientUser(final Client client, final String username) {

        final ClientUser.ClientUserId clientUserId = new ClientUser.ClientUserId(username, client.getUsername());
        return clientUserRepository.findById(clientUserId).orElseThrow(() -> {
            throw new ObjectNotFoundException(clientUserId.toString(), ClientUser.class);
        });
    }

    @Override
    public List<ClientUser> getClientUsers(final Client client) {
        return clientUserRepository.findAllByClientId(client.getUsername());
    }

    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {

        final String clientUsername = findCurrentClientUsername();
        final ClientUser clientUser = clientUserRepository.findById(new ClientUser.ClientUserId(username, clientUsername)).orElseThrow(() -> {
            throw new UsernameNotFoundException("Username does not exist: " + username);
        });

        final Collection<? extends GrantedAuthority> authorities = AuthorityUtils.commaSeparatedStringToAuthorityList(clientUser.getRoles());
        return new User(clientUser.getId().getUsername(), clientUser.getPassword(), authorities);
    }

    /**
     * This only works during /oauth/token.
     */
    private String findCurrentClientUsername() {

        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null) {
            final Object principal = authentication.getPrincipal();

            if (principal instanceof User) {
                return ((User) principal).getUsername();
            }
        }

        return null;
    }
}
