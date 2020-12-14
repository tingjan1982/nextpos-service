package io.nextpos.client.web;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.client.web.model.ClientResponse;
import io.nextpos.client.web.model.ClientsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/clients")
public class ClientAdminController {

    private final ClientService clientService;

    @Autowired
    public ClientAdminController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    public ClientsResponse getClients() {

        final List<ClientResponse> results = clientService.getClients().stream()
                .map(ClientResponse::new)
                .collect(Collectors.toList());

        return new ClientsResponse(results);
    }

    @GetMapping("/{id}")
    public ClientResponse getClient(@PathVariable String id) {

        final Client client = clientService.getClientOrThrows(id);
        final ClientResponse clientResponse = new ClientResponse(client);

        if (client.getClientInfo() != null) {
            clientResponse.setClientInfo(new ClientResponse.ClientInfoResponse(client.getClientInfo()));
        }
        
        return clientResponse;
    }
}
