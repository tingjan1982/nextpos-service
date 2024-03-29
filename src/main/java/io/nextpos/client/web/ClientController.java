package io.nextpos.client.web;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientInfo;
import io.nextpos.client.data.ClientUser;
import io.nextpos.client.service.ClientActivationService;
import io.nextpos.client.service.ClientBootstrapService;
import io.nextpos.client.service.ClientService;
import io.nextpos.client.service.DeleteClientService;
import io.nextpos.client.web.model.*;
import io.nextpos.clienttracker.data.ClientUsageTrack;
import io.nextpos.clienttracker.service.ClientUserTrackingService;
import io.nextpos.einvoice.common.encryption.EncryptionService;
import io.nextpos.roles.data.UserRole;
import io.nextpos.roles.service.UserRoleService;
import io.nextpos.settings.service.SettingsService;
import io.nextpos.shared.config.BootstrapConfig;
import io.nextpos.shared.exception.BusinessLogicException;
import io.nextpos.shared.exception.ClientAccountException;
import io.nextpos.shared.exception.GeneralApplicationException;
import io.nextpos.shared.web.ClientResolver;
import io.nextpos.subscription.data.ClientSubscriptionAccess;
import io.nextpos.subscription.service.ClientSubscriptionAccessService;
import io.nextpos.workingarea.data.WorkingArea;
import io.nextpos.workingarea.service.WorkingAreaService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.common.util.RandomValueStringGenerator;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/clients")
public class ClientController {

    private final ClientService clientService;

    private final DeleteClientService deleteClientService;

    private final UserRoleService userRoleService;

    private final SettingsService settingsService;

    private final WorkingAreaService workingAreaService;

    private final ClientBootstrapService clientBootstrapService;

    private final ClientActivationService clientActivationService;

    private final ClientUserTrackingService clientUserTrackingService;

    private final ClientSubscriptionAccessService clientSubscriptionAccessService;

    private final EncryptionService encryptionService;

    @Autowired
    public ClientController(final ClientService clientService, DeleteClientService deleteClientService, final UserRoleService userRoleService, SettingsService settingsService, WorkingAreaService workingAreaService, ClientBootstrapService clientBootstrapService, final ClientActivationService clientActivationService, ClientUserTrackingService clientUserTrackingService, ClientSubscriptionAccessService clientSubscriptionAccessService, EncryptionService encryptionService) {
        this.clientService = clientService;
        this.deleteClientService = deleteClientService;
        this.userRoleService = userRoleService;
        this.settingsService = settingsService;
        this.workingAreaService = workingAreaService;
        this.clientBootstrapService = clientBootstrapService;
        this.clientActivationService = clientActivationService;
        this.clientUserTrackingService = clientUserTrackingService;
        this.clientSubscriptionAccessService = clientSubscriptionAccessService;
        this.encryptionService = encryptionService;
    }

    @PostMapping
    public ClientResponse createClient(@Valid @RequestBody ClientRequest clientRequest) {

        final Client client = fromClientRequest(clientRequest);
        updateClientInfoFromRequest(client, clientRequest.getClientInfo());

        final Client createdClient = clientService.createClient(client);

        clientActivationService.initiateClientActivation(createdClient);
        clientBootstrapService.bootstrapClient(createdClient);

        return toClientResponse(createdClient);

    }

    private Client fromClientRequest(ClientRequest clientRequest) {

        final Client client = new Client(clientRequest.getClientName().trim(),
                clientRequest.getUsername().trim(),
                clientRequest.getMasterPassword(),
                BootstrapConfig.DEFAULT_COUNTRY_CODE,
                BootstrapConfig.DEFAULT_TIME_ZONE);

        if (!CollectionUtils.isEmpty(clientRequest.getAttributes())) {
            clientRequest.getAttributes().forEach(client::addAttribute);
        }

        client.addSupportedPaymentMethod(settingsService.getPaymentMethodByPaymentKey("CASH"));
        client.addSupportedPaymentMethod(settingsService.getPaymentMethodByPaymentKey("CARD"));

        return client;
    }

    @GetMapping("/me")
    public ClientResponse getCurrentClient(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        final ClientResponse response = toClientResponse(client);
        final ClientSubscriptionAccess clientSubscriptionAccess = clientSubscriptionAccessService.getClientSubscriptionAccess(client.getId());
        response.setClientSubscriptionAccess(clientSubscriptionAccess);

        return response;
    }

    @GetMapping("/{id}")
    public ClientResponse getClient(@PathVariable String id) {

        final Client client = clientService.getClientOrThrows(id);

        return toClientResponse(client);
    }

    @PostMapping("/me")
    public ClientResponse updateClient(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                       @Valid @RequestBody UpdateClientRequest updateClientRequest) {

        updateClientFromRequest(client, updateClientRequest);
        clientService.saveClient(client);

        return toClientResponse(client);
    }

    private void updateClientFromRequest(final Client client, final UpdateClientRequest updateClientRequest) {
        client.setClientName(updateClientRequest.getClientName());
        client.setTimezone(updateClientRequest.getTimezone());
        client.setAttributes(updateClientRequest.getAttributes());

        if (updateClientRequest.getClientSettings() != null) {
            updateClientRequest.getClientSettings().forEach((k, v) -> client.saveClientSettings(k, v.getValue(), v.isEnabled()));
        }

        client.clearPaymentMethods();

        updateClientRequest.getPaymentMethodIds().forEach(id -> settingsService.getPaymentMethod(id).ifPresent(client::addSupportedPaymentMethod));
    }

    @GetMapping("/me/info")
    public ClientInfoResponse getClientInfo(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        return new ClientInfoResponse(client.getClientInfo());
    }

    @PostMapping("/me/info")
    public ClientInfoResponse updateClientInfo(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                               @Valid @RequestBody ClientInfoRequest request) {

        updateClientInfoFromRequest(client, request);
        clientService.saveClient(client);

        return new ClientInfoResponse(client.getClientInfo());
    }

    private void updateClientInfoFromRequest(Client client, ClientInfoRequest request) {

        if (request != null) {
            ClientInfo clientInfo = client.getClientInfo();

            if (clientInfo == null) {
                clientInfo = new ClientInfo();
                client.updateClientInfo(clientInfo);
            }

            clientInfo.setOwnerName(request.getOwnerName());
            clientInfo.setContactNumber(request.getContactNumber());
            clientInfo.setContactAddress(request.getContactAddress());
            clientInfo.setOperationStatus(request.getOperationStatus());
            clientInfo.setLeadSource(request.getLeadSource());
            clientInfo.setRequirements(request.getRequirements());
        }
    }

    @PostMapping("/me/attributes")
    public ClientResponse updateClientAttribute(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                @Valid @RequestBody UpdateClientAttributeRequest request) {

        request.getAttributes().forEach(client::addAttribute);

        return toClientResponse(clientService.saveClient(client));
    }

    @PatchMapping("/me/clientType")
    public ClientResponse updateClientType(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                           @Valid @RequestBody UpdateClientTypeRequest request) {

        client.setClientType(request.getClientType());

        return toClientResponse(clientService.saveClient(client));
    }

    @PostMapping("/me/aeskey")
    public ClientResponse generateAESKey(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                         @RequestBody String password) {

        final String aesKey = encryptionService.generateAESKey(password);
        client.addAttribute(Client.ClientAttributes.AES_KEY, aesKey);
        clientService.saveClient(client);

        return toClientResponse(client);
    }

    /**
     * Client can choose to terminate account by themselves.
     *
     * @param client
     */
    @DeleteMapping("/me")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void markClientAsDeleted(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                    @RequestBody String clientName) {

        if (!StringUtils.equals(clientName, client.getClientName())) {
            throw new ClientAccountException("Delete client failed due to client name mismatch", client);
        }

        clientService.updateClientStatus(client, Client.Status.DELETED);
    }

    @DeleteMapping("/me/hard")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void hardDeleteClient(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        if (client.getStatus() != Client.Status.DELETED) {
            throw new BusinessLogicException("You have to mark client account as deleted first.");
        }

        deleteClientService.deleteClient(client.getId());
    }

    private ClientResponse toClientResponse(final Client client) {

        return new ClientResponse(client);
    }

    @PostMapping("/me/users")
    public ClientUserResponse createClientUser(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                               @Valid @RequestBody ClientUserRequest clientUserRequest) {

        final ClientUser clientUser = fromClientUserRequest(client, clientUserRequest);
        final ClientUser createdClientUser = clientService.createClientUser(clientUser);

        return toClientUserResponse(createdClientUser);
    }

    private ClientUser fromClientUserRequest(Client client, ClientUserRequest request) {

        final String roles = String.join(",", request.getRoles());
        String username = new RandomValueStringGenerator(16).generate();
        final ClientUser clientUser = new ClientUser(client, username, request.getNickname(), request.getPassword(), roles);

        if (StringUtils.isNotBlank(request.getNickname())) {
            clientUser.setNickname(request.getNickname().trim());
        }

        if (StringUtils.isNotBlank(request.getUserRoleId())) {
            final UserRole userRole = userRoleService.loadUserRole(request.getUserRoleId());
            clientUser.setUserRole(userRole);
        }

        request.getWorkingAreaIds().forEach(id -> {
            final WorkingArea workingArea = workingAreaService.getWorkingArea(id);
            clientUser.addWorkingArea(workingArea);
        });

        return clientUser;
    }

    @GetMapping("/me/users/{username}")
    public ClientUserResponse getClientUser(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                            @PathVariable final String username) {

        final ClientUser clientUser;

        if (username.equals("me")) {
            clientUser = clientService.getCurrentClientUser(client);
        } else {
            clientUser = clientService.getClientUser(client, username);
        }

        return toClientUserResponse(clientUser);
    }

    @GetMapping("/me/users")
    public ClientUsersResponse getClientUsers(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        final List<ClientUser> clientUsers = clientService.getClientUsers(client);
        final List<ClientUserResponse> clientUsersResponse = clientUsers.stream()
                .map(this::toClientUserResponse).collect(Collectors.toList());

        return new ClientUsersResponse(clientUsersResponse);
    }

    @PostMapping("/me/users/{username}")
    public ClientUserResponse updateClientUser(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                               @PathVariable final String username,
                                               @Valid @RequestBody UpdateClientUserRequest updateClientUserRequest) {

        final ClientUser clientUser = clientService.loadClientUser(client, username);

        if (clientUser.isDefaultUser()) {
            throw new GeneralApplicationException("Default client user cannot be updated.");
        }

        updateClientUserFromRequest(clientUser, updateClientUserRequest);

        return toClientUserResponse(clientService.saveClientUser(clientUser));
    }

    private void updateClientUserFromRequest(final ClientUser clientUser, final UpdateClientUserRequest request) {

        clientUser.setNickname(request.getNickname());
        final String roles = String.join(",", request.getRoles());
        clientUser.setRoles(roles);

        if (StringUtils.isNotBlank(request.getUserRoleId())) {
            final UserRole userRole = userRoleService.loadUserRole(request.getUserRoleId());
            clientUser.setUserRole(userRole);
        } else {
            userRoleService.removeClientUserRole(clientUser);
        }

        clientUser.clearWorkingAreas();

        request.getWorkingAreaIds().forEach(id -> {
            final WorkingArea workingArea = workingAreaService.getWorkingArea(id);
            clientUser.addWorkingArea(workingArea);
        });
    }

    @PostMapping("/me/users/{username}/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void userLogout(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                           @PathVariable final String username) {

        clientUserTrackingService.deleteClientUsageTrack(client, ClientUsageTrack.TrackingType.USER, username);
    }

    @PatchMapping(value = "/me/users/{username}/password")
    public ClientUserResponse updateClientUserPassword(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                       @PathVariable final String username,
                                                       @Valid @RequestBody UpdateClientUserPasswordRequest request) {

        final ClientUser clientUser = clientService.getClientUser(client, username);

        if (clientUser.isDefaultUser()) {
            throw new GeneralApplicationException("Default client user cannot be updated.");
        }

        clientService.updateClientUserPassword(client, clientUser, request.getPassword());

        return toClientUserResponse(clientUser);
    }

    @PatchMapping("/me/users/currentUser/password")
    public ClientUserResponse updateCurrentClientUserPassword(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                              @Valid @RequestBody UpdateClientUserPasswordRequest request) {

        final ClientUser currentUser = clientService.getCurrentClientUser(client);

        final ClientUser updatedClientUser = clientService.updateClientUserPassword(client, currentUser, request.getPassword());

        return toClientUserResponse(updatedClientUser);
    }

    @DeleteMapping("/me/users/{username}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteClientUser(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                 @PathVariable final String username) {

        clientService.deleteClientUser(client, username);
    }

    private ClientUserResponse toClientUserResponse(ClientUser clientUser) {

        final List<String> roles = Arrays.asList(clientUser.getRoles().split(","));
        return new ClientUserResponse(clientUser.getId(),
                clientUser.getName(),
                clientUser.getNickname(),
                clientUser.getUsername(),
                clientUser.getPassword(),
                roles,
                clientUser.getUserRole() != null ? clientUser.getUserRole().getId() : null,
                clientUser.getWorkingAreas().stream().map(WorkingArea::getId).collect(Collectors.toList()),
                clientUser.isDefaultUser(),
                clientUser.getPermissions());
    }
}
