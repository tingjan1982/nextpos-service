package io.nextpos.client.service;

import io.micrometer.core.instrument.util.StringUtils;
import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientRepository;
import io.nextpos.client.data.ClientUser;
import io.nextpos.client.data.ClientUserRepository;
import io.nextpos.linkedaccount.service.LinkedClientAccountService;
import io.nextpos.roles.data.Permission;
import io.nextpos.shared.config.BootstrapConfig;
import io.nextpos.shared.config.SecurityConfig;
import io.nextpos.shared.exception.BusinessLogicException;
import io.nextpos.shared.exception.ObjectAlreadyExistsException;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.service.annotation.JpaTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.NoSuchClientException;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@JpaTransaction
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;

    private final ClientUserRepository clientUserRepository;

    private final JdbcClientDetailsService clientDetailsService;

    private final PasswordEncoder passwordEncoder;

    private final LinkedClientAccountService linkedClientAccountService;

    @Autowired
    public ClientServiceImpl(final ClientRepository clientRepository, final ClientUserRepository clientUserRepository, JdbcClientDetailsService clientDetailsService, PasswordEncoder passwordEncoder, LinkedClientAccountService linkedClientAccountService) {
        this.clientRepository = clientRepository;
        this.clientUserRepository = clientUserRepository;
        this.clientDetailsService = clientDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.linkedClientAccountService = linkedClientAccountService;
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

        final String roles = String.join(",", SecurityConfig.Role.getRoles());
        final String permissions = clientDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.joining(","));

        // client's default user will use email as the username.
        final ClientUser defaultClientUser = new ClientUser(new ClientUser.ClientUserId(client.getUsername(), client.getUsername()), client, plainPassword, roles);
        defaultClientUser.setNickname(client.getClientName());
        defaultClientUser.setPermissions(permissions);

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
        result.setScope(Permission.allPermissions());

        final ArrayList<String> authorities = new ArrayList<>(Permission.allPermissions());
        authorities.addAll(SecurityConfig.Role.getRoles());

        if (StringUtils.isNotBlank(client.getRoles())) {
            authorities.add(client.getRoles());
        }

        result.setAuthorities(AuthorityUtils.createAuthorityList(authorities.toArray(String[]::new)));
        result.setResourceIds(Collections.singletonList(SecurityConfig.OAuthSettings.RESOURCE_ID));

        return result;
    }

    @Override
    public Client saveClient(final Client client) {
        return clientRepository.save(client);
    }

    @Override
    public Client authenticateClient(String clientId, String password) {

        final ClientDetails clientDetails = clientDetailsService.loadClientByClientId(clientId);

        if (!passwordEncoder.matches(password, clientDetails.getClientSecret())) {
            throw new BusinessLogicException("message.authenticationFailed", "Client credentials failed");
        }

        return getClientByUsername(clientId).orElseThrow(() -> {
            throw new ObjectNotFoundException(clientId, Client.class);
        });
    }

    @Override
    public ClientUser updateClientUserPassword(Client client, ClientUser clientUser, String newPassword) {

        clientUser.setPassword(newPassword);
        final ClientUser updatedClientUser = this.saveClientUser(clientUser);

        if (clientUser.isDefaultUser()) {
            this.updateDefaultClientUserPassword(client, newPassword);
        }

        return updatedClientUser;
    }

    @Override
    public void updateDefaultClientUserPassword(Client client, String newPassword) {
        final String username = client.getUsername();
        clientDetailsService.updateClientSecret(username, newPassword);
    }

    @Override
    public Optional<Client> getClient(final String clientId) {
        return clientRepository.findByIdAndStatusIn(clientId, Client.Status.ACTIVE, Client.Status.PENDING_ACTIVE);
    }

    @Override
    public Client getClientOrThrows(String clientId) {
        return this.getClient(clientId).orElseThrow(() -> {
            throw new ObjectNotFoundException(clientId, Client.class);
        });
    }

    @Override
    public Optional<Client> getClientByStatuses(final String clientId, final Client.Status... status) {
        return clientRepository.findByIdAndStatusIn(clientId, status);
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
    public List<Client> getClients() {
        return clientRepository.findAll();
    }

    @Override
    public void updateClientStatus(final Client client, final Client.Status status) {

        client.setStatus(status);
        clientRepository.save(client);
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

        clientUserRepository.findByIdAndClient(clientUser.getId(), clientUser.getClient()).ifPresent(u -> {
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
    public ClientUser loadClientUser(Client client, String username) {

        final ClientUser.ClientUserId clientUserId = new ClientUser.ClientUserId(username, client.getUsername());
        return clientUserRepository.loadById(clientUserId).orElseThrow(() -> {
            throw new ObjectNotFoundException(clientUserId.toString(), ClientUser.class);
        });
    }

    @Override
    public List<ClientUser> getClientUsers(final Client client) {
        return linkedClientAccountService.getLinkedClientAccountResources(client, clientUserRepository::findAllByClientIn);
    }

    /**
     * The password check avoids encrypting an already encrypted password.
     *
     * @param clientUser
     * @return
     */
    @Override
    public ClientUser saveClientUser(final ClientUser clientUser) {

        if (!clientUser.getPassword().startsWith("{bcrypt}")) {
            final String encryptedPassword = passwordEncoder.encode(clientUser.getPassword());
            clientUser.setPassword(encryptedPassword);
        }

        return clientUserRepository.save(clientUser);
    }

    @Override
    public void deleteClientUser(final Client client, final String username) {

        final ClientUser clientUser = this.getClientUser(client, username);
        clientUserRepository.delete(clientUser);
    }

    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {

        final String clientUsername = findCurrentClientUsername();
        final Client client = this.getClientByUsername(clientUsername).orElseThrow(() -> {
            throw new UsernameNotFoundException("Client cannot be resolved by the username: " + username);
        });

        final ClientUser clientUser = linkedClientAccountService.getLinkedClientAccountResources(client, (clients) -> clientUserRepository.findByIdUsernameAndClientIn(username, clients).orElseThrow(() -> {
            throw new UsernameNotFoundException("Username does not exist: " + username);
        }));

        final String joinedAuthorities = clientUser.getRoles() + "," + clientUser.getPermissions();
        final Collection<? extends GrantedAuthority> authorities = AuthorityUtils.commaSeparatedStringToAuthorityList(joinedAuthorities);
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
