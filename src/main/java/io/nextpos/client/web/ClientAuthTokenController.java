package io.nextpos.client.web;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientAuthTokenService;
import io.nextpos.client.service.ClientService;
import io.nextpos.client.service.bean.ClientAuthToken;
import io.nextpos.client.web.model.ClientAuthTokenRequest;
import io.nextpos.client.web.model.DecodeAuthTokenRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/tokens")
public class ClientAuthTokenController {

    private final ClientService clientService;
    
    private final ClientAuthTokenService clientAuthTokenService;

    @Autowired
    public ClientAuthTokenController(ClientService clientService, ClientAuthTokenService clientAuthTokenService) {
        this.clientService = clientService;
        this.clientAuthTokenService = clientAuthTokenService;
    }

    @PostMapping("/encode")
    public String encodeClientAuthToken(@Valid @RequestBody ClientAuthTokenRequest request) {

        final Client client = clientService.authenticateClient(request.getUsername(), request.getPassword());

        return clientAuthTokenService.encodeClientAuthToken(client, request.getPassword());
    }

    @PostMapping("/decode")
    public ClientAuthToken decodeClientAuthToken(@Valid @RequestBody DecodeAuthTokenRequest request) {

        return clientAuthTokenService.decodeClientAuthToken(request.getToken());
    }
}
