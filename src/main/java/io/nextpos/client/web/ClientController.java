package io.nextpos.client.web;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.client.service.ClientActivationService;
import io.nextpos.client.service.ClientService;
import io.nextpos.client.web.model.*;
import io.nextpos.shared.config.BootstrapConfig;
import io.nextpos.shared.exception.ClientAccountException;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.web.ClientResolver;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

    private final ClientActivationService clientActivationService;

    @Autowired
    public ClientController(final ClientService clientService, final ClientActivationService clientActivationService) {
        this.clientService = clientService;
        this.clientActivationService = clientActivationService;
    }

    @PostMapping
    public ClientResponse createClient(@Valid @RequestBody ClientRequest clientRequest) {

        final Client client = fromClientRequest(clientRequest);
        final Client createdClient = clientService.createClient(client);

        clientActivationService.sendActivationNotification(createdClient);

        return toClientResponse(createdClient);

    }

    @GetMapping("/me")
    public ClientResponse getCurrentClient(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        return toClientResponse(client);
    }

    @GetMapping("/{id}")
    public ClientResponse getClient(@PathVariable String id) {

        final Client client = clientService.getClient(id).orElseThrow(() -> {
            throw new ObjectNotFoundException(id, Client.class);
        });

        return toClientResponse(client);
    }

    @GetMapping("/default")
    public ClientResponse getTestClient() {

        return toClientResponse(clientService.getDefaultClient());
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
        client.setAttributes(updateClientRequest.getAttributes());
    }

    @PostMapping("/{id}/deactivate")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void deactivateClient(@PathVariable String id) {

        final Client client = clientService.getClient(id).orElseThrow(() -> {
            throw new ClientAccountException("Specified client account is not active.", id);
        });
        
        clientService.updateClientStatus(client, Client.Status.INACTIVE);
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

    @DeleteMapping("/{id}/hard")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void hardDeleteClient(@PathVariable String id) {
        clientService.deleteClient(id);
    }

    private Client fromClientRequest(ClientRequest clientRequest) {

        final Client client = new Client(clientRequest.getClientName(),
                clientRequest.getUsername(),
                clientRequest.getMasterPassword(),
                BootstrapConfig.DEFAULT_COUNTRY_CODE);

        if (!CollectionUtils.isEmpty(clientRequest.getAttributes())) {
            clientRequest.getAttributes().forEach(client::addAttribute);
        }

        return client;
    }

    private ClientResponse toClientResponse(final Client client) {

        return new ClientResponse(client.getId(),
                client.getClientName(),
                client.getUsername(),
                client.getMasterPassword(),
                client.getCountryCode(),
                client.getStatus(),
                client.getAttributes());
    }

    @PostMapping("/me/users")
    public ClientUserResponse createClientUser(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client, @Valid @RequestBody ClientUserRequest clientUserRequest) {

        final ClientUser clientUser = fromClientUserRequest(client, clientUserRequest);
        final ClientUser createdClientUser = clientService.createClientUser(clientUser);

        return toClientUserResponse(createdClientUser);
    }

    @GetMapping("/me/users")
    public ClientUsersResponse getClientUsers(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        final List<ClientUser> clientUsers = clientService.getClientUsers(client);
        final List<ClientUserResponse> clientUsersResponse = clientUsers.stream()
                .map(this::toClientUserResponse).collect(Collectors.toList());

        return new ClientUsersResponse(clientUsersResponse);
    }

    private ClientUser fromClientUserRequest(Client client, ClientUserRequest clientUserRequest) {

        final String roles = String.join(",", clientUserRequest.getRoles());
        final ClientUser.ClientUserId id = new ClientUser.ClientUserId(clientUserRequest.getUsername(), client.getUsername());
        final ClientUser clientUser = new ClientUser(id, clientUserRequest.getPassword(), roles);
        clientUser.setNickname(clientUserRequest.getNickname());

        return clientUser;
    }

    private ClientUserResponse toClientUserResponse(ClientUser clientUser) {

        final List<String> roles = Arrays.asList(clientUser.getRoles().split(","));
        return new ClientUserResponse(clientUser.getNickname(), clientUser.getId().getUsername(), clientUser.getPassword(), roles);
    }
}
