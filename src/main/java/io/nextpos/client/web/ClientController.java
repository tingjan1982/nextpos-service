package io.nextpos.client.web;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.client.service.ClientService;
import io.nextpos.client.web.model.ClientRequest;
import io.nextpos.client.web.model.ClientResponse;
import io.nextpos.client.web.model.ClientUserRequest;
import io.nextpos.client.web.model.ClientUserResponse;
import io.nextpos.shared.config.BootstrapConfig;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.web.ClientResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/clients")
public class ClientController {

    private final ClientService clientService;


    @Autowired
    public ClientController(final ClientService clientService) {
        this.clientService = clientService;
    }

    @PostMapping
    public ClientResponse createClient(@Valid @RequestBody ClientRequest clientRequest) {

        final Client client = fromClientRequest(clientRequest);
        final Client createdClient = clientService.createClient(client);

        return toClientResponse(createdClient);

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

    @DeleteMapping("/{id}")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void deleteClient(@PathVariable String id) {
        clientService.markClientAsDeleted(id);
    }


    @DeleteMapping("/{id}/hard")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void hardDeleteClient(@PathVariable String id) {
        clientService.deleteClient(id);
    }

    private Client fromClientRequest(ClientRequest clientRequest) {

        return new Client(clientRequest.getClientName(),
                clientRequest.getUsername(),
                clientRequest.getMasterPassword(),
                BootstrapConfig.DEFAULT_COUNTRY_CODE);
    }

    private ClientResponse toClientResponse(final Client client) {

        return new ClientResponse(client.getId(), client.getClientName(), client.getUsername(), client.getMasterPassword(), client.getCountryCode(), client.getStatus());
    }

    @PostMapping("/me/users")
    public ClientUserResponse createClientUser(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client, @RequestBody ClientUserRequest clientUserRequest) {

        final ClientUser clientUser = fromClientUserRequest(client, clientUserRequest);
        final ClientUser createdClientUser = clientService.createClientUser(clientUser);

        return toClientUserResponse(createdClientUser);
    }

    private ClientUser fromClientUserRequest(Client client, ClientUserRequest clientUserRequest) {

        final String roles = String.join(",", clientUserRequest.getRoles());
        final ClientUser.ClientUserId id = new ClientUser.ClientUserId(clientUserRequest.getUsername(), client.getUsername());
        return new ClientUser(id, clientUserRequest.getPassword(), roles);
    }

    private ClientUserResponse toClientUserResponse(ClientUser clientUser) {

        final List<String> roles = Arrays.asList(clientUser.getRoles().split(","));
        return new ClientUserResponse(clientUser.getId().getUsername(), clientUser.getPassword(), roles);
    }
}
