package io.nextpos.client.web;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.client.service.ClientService;
import io.nextpos.client.web.model.*;
import io.nextpos.ordermanagement.data.OrderIdCounter;
import io.nextpos.ordermanagement.service.OrderCounterService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/clients")
public class ClientAdminController {

    private final ClientService clientService;

    private final OrderCounterService orderCounterService;

    @Autowired
    public ClientAdminController(ClientService clientService, OrderCounterService orderCounterService) {
        this.clientService = clientService;
        this.orderCounterService = orderCounterService;
    }

    @GetMapping
    public ClientsResponse getClients() {

        final Map<String, OrderIdCounter.OrderCounterSummary> orderCounterSummaries = orderCounterService.getOrderCounterSummaries();
        final List<ClientsResponse.ClientDetailsResponse> results = clientService.getClients().stream()
                .map(c -> {
                    final OrderIdCounter.OrderCounterSummary orderCounterSummary = orderCounterSummaries.getOrDefault(c.getId(), new OrderIdCounter.OrderCounterSummary());
                    return new ClientsResponse.ClientDetailsResponse(c, orderCounterSummary);
                })
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

    @PatchMapping("/{id}/username")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateClientUsername(@PathVariable String id,
                                     @Valid @RequestBody UpdateClientUsernameRequest request) {

        clientService.getClient(id).ifPresent(c -> {
            clientService.updateUsernameForClient(c, request.getNewUsername(), request.getPassword());

            if (StringUtils.isNotBlank(request.getNewClientName())) {
                c.setClientName(request.getNewClientName());
                clientService.saveClient(c);
            }
        });
    }

    @PatchMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateClientPassword(@PathVariable String id,
                                     @Valid @RequestBody UpdateClientUserPasswordRequest request) {

        clientService.getClient(id).ifPresent(c -> {
            final ClientUser defaultClientUser = clientService.getClientUser(c, c.getUsername());
            clientService.updateClientUserPassword(c, defaultClientUser, request.getPassword());
        });
    }

    @PatchMapping("/{id}/clientName")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateClientName(@PathVariable String id,
                                 @Valid @RequestBody UpdateClientNameRequest request) {

        clientService.getClient(id).ifPresent(c -> {
            c.setClientName(request.getClientName());
            clientService.saveClient(c);
        });
    }
}
