package io.nextpos.client.web;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.client.web.model.ClientRequest;
import io.nextpos.client.web.model.ClientResponse;
import io.nextpos.shared.exception.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clients")
public class ClientController {

    private final ClientService clientService;

    @Autowired
    public ClientController(final ClientService clientService) {
        this.clientService = clientService;
    }

    @PostMapping
    public ClientResponse createClient(@RequestBody ClientRequest clientRequest) {

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


    private Client fromClientRequest(ClientRequest clientRequest) {

        return new Client(clientRequest.getClientName(),
                clientRequest.getUsername(),
                clientRequest.getMasterPassword());
    }

    private ClientResponse toClientResponse(final Client client) {

        return new ClientResponse(client.getId(), client.getClientName(), client.getUsername(), client.getMasterPassword());
    }
}
