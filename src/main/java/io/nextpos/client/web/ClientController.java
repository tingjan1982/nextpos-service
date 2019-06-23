package io.nextpos.client.web;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clients")
public class ClientController {

    private final ClientService clientService;

    @Autowired
    public ClientController(final ClientService clientService) {
        this.clientService = clientService;
    }


    @GetMapping("/default")
    public Client getTestClient() {
        return clientService.getDefaultClient();
    }
}
