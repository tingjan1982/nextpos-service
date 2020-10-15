package io.nextpos.subscription.web;

import io.nextpos.client.data.Client;
import io.nextpos.shared.web.ClientResolver;
import io.nextpos.subscription.data.ClientSubscription;
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

        final ClientSubscription clientSubscription = clientSubscriptionService.createClientSubscription(client, request.getSubscriptionPlanId(), request.getPlanPeriod());

        return toResponse(clientSubscription);
    }

    @GetMapping("/current")
    public ClientSubscriptionResponse getCurrentClientSubscription(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        return toResponse(clientSubscriptionService.getCurrentClientSubscription(client.getId()));
    }

    private ClientSubscriptionResponse toResponse(ClientSubscription clientSubscription) {
        return new ClientSubscriptionResponse(clientSubscription.getId(),
                clientSubscription.getSubscriptionPlanSnapshot().getPlanName(),
                clientSubscription.getStatus(),
                clientSubscription.getPlanPrice(),
                clientSubscription.getPlanPeriod(),
                clientSubscription.getSubmittedDate(),
                clientSubscription.getPlanStartDate(),
                clientSubscription.getPlanEndDate());
    }
}
