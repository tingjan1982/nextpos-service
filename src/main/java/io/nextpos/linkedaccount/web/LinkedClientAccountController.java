package io.nextpos.linkedaccount.web;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.linkedaccount.data.LinkedClientAccount;
import io.nextpos.linkedaccount.service.LinkedClientAccountService;
import io.nextpos.linkedaccount.web.model.LinkedClientAccountRequest;
import io.nextpos.linkedaccount.web.model.LinkedClientAccountResponse;
import io.nextpos.shared.web.ClientResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/linkedClientAccounts")
public class LinkedClientAccountController {

    private final LinkedClientAccountService linkedClientAccountService;

    private final ClientService clientService;

    @Autowired
    public LinkedClientAccountController(LinkedClientAccountService linkedClientAccountService, ClientService clientService) {
        this.linkedClientAccountService = linkedClientAccountService;
        this.clientService = clientService;
    }

    @PostMapping
    public LinkedClientAccountResponse createLinkedClientAccount(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                                 @Valid @RequestBody LinkedClientAccountRequest request) {

        final Client clientToLink = clientService.authenticateClient(request.getClientUsername(), request.getClientPassword());

        final LinkedClientAccount linkedClientAccount = linkedClientAccountService.createLinkedClientAccount(client, clientToLink);

        return new LinkedClientAccountResponse(linkedClientAccount);
    }

    @GetMapping("/me")
    public LinkedClientAccountResponse getLinkedClientAccount(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        final LinkedClientAccount linkedClientAccount = linkedClientAccountService.getLinkedClientAccountOrThrows(client);

        return new LinkedClientAccountResponse(linkedClientAccount);
    }

    @PostMapping("/me/linkedClients")
    public LinkedClientAccountResponse addLinkedClient(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                       @Valid @RequestBody LinkedClientAccountRequest request) {

        final LinkedClientAccount linkedClientAccount = linkedClientAccountService.getLinkedClientAccountOrThrows(client);
        final Client clientToLink = clientService.authenticateClient(request.getClientUsername(), request.getClientPassword());

        return new LinkedClientAccountResponse(linkedClientAccountService.addLinkedClient(linkedClientAccount, clientToLink));
    }

    @DeleteMapping("/me/linkedClients/{clientId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeLinkedClient(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                   @PathVariable String clientId) {

        final LinkedClientAccount linkedClientAccount = linkedClientAccountService.getLinkedClientAccountOrThrows(client);
        clientService.getClient(clientId).ifPresent(c -> linkedClientAccountService.removeLinkedClient(linkedClientAccount, c));
    }
}
