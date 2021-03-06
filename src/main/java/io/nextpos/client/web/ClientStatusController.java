package io.nextpos.client.web;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientStatus;
import io.nextpos.client.service.ClientStatusService;
import io.nextpos.client.web.model.ClientStatusResponse;
import io.nextpos.shared.web.ClientResolver;
import io.nextpos.subscription.service.ClientSubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clientstatus")
public class ClientStatusController {

    private final ClientStatusService clientStatusService;

    private final ClientSubscriptionService clientSubscriptionService;

    @Autowired
    public ClientStatusController(ClientStatusService clientStatusService, ClientSubscriptionService clientSubscriptionService) {
        this.clientStatusService = clientStatusService;
        this.clientSubscriptionService = clientSubscriptionService;
    }

    @GetMapping("/me")
    public ClientStatusResponse checkClientStatus(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        return toClientStatusResponse(clientStatusService.checkClientStatus(client));
    }

    private ClientStatusResponse toClientStatusResponse(ClientStatus clientStatus) {

        return new ClientStatusResponse(
                clientStatus.getClient().getStatus() == Client.Status.PENDING_ACTIVE,
                clientStatus.isNoTableLayout(),
                clientStatus.isNoTable(),
                clientStatus.isNoCategory(),
                clientStatus.isNoProduct(),
                clientStatus.isNoWorkingArea(),
                clientStatus.isNoPrinter(),
                clientStatus.isNoElectronicInvoice());
    }
}
