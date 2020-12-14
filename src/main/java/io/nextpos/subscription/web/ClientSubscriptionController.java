package io.nextpos.subscription.web;

import io.nextpos.client.data.Client;
import io.nextpos.shared.web.ClientResolver;
import io.nextpos.subscription.data.ClientSubscription;
import io.nextpos.subscription.data.ClientSubscriptionInvoice;
import io.nextpos.subscription.service.ClientSubscriptionService;
import io.nextpos.subscription.web.model.ClientSubscriptionRequest;
import io.nextpos.subscription.web.model.ClientSubscriptionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clientSubscriptions")
public class ClientSubscriptionController {

    private final ClientSubscriptionService clientSubscriptionService;

    @Autowired
    public ClientSubscriptionController(ClientSubscriptionService clientSubscriptionService) {
        this.clientSubscriptionService = clientSubscriptionService;
    }

    @PostMapping
    public ClientSubscriptionResponse submitClientSubscription(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                               @RequestBody ClientSubscriptionRequest request) {

        final ClientSubscriptionInvoice invoice = clientSubscriptionService.createClientSubscription(client, request.getSubscriptionPlanId(), request.getPlanPeriod());
        final ClientSubscriptionResponse response = toResponse(invoice.getClientSubscription());
        response.setInvoiceIdentifier(invoice.getInvoiceIdentifier());

        return response;
    }

    @GetMapping("/current")
    public ClientSubscriptionResponse getCurrentClientSubscription(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        return toResponse(clientSubscriptionService.getCurrentClientSubscription(client.getId()));
    }

    @PostMapping("/current/lapse")
    public ClientSubscriptionResponse lapseCurrentClientSubscription(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        return null;
    }

    @PostMapping("/current/cancel")
    public ClientSubscriptionResponse cancelCurrentClientSubscription(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        return null;
    }

    private ClientSubscriptionResponse toResponse(ClientSubscription clientSubscription) {

        return new ClientSubscriptionResponse(clientSubscription);
    }
}
