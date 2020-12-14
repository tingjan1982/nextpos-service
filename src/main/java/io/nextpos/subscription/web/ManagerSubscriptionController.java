package io.nextpos.subscription.web;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.subscription.data.ClientSubscription;
import io.nextpos.subscription.data.ClientSubscriptionInvoice;
import io.nextpos.subscription.service.ClientSubscriptionService;
import io.nextpos.subscription.web.model.ClientSubscriptionInvoiceResponse;
import io.nextpos.subscription.web.model.ClientSubscriptionInvoicesResponse;
import io.nextpos.subscription.web.model.ClientSubscriptionResponse;
import io.nextpos.subscription.web.model.ClientSubscriptionsResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
public class ManagerSubscriptionController {

    private final ClientSubscriptionService clientSubscriptionService;

    private final ClientService clientService;

    @Autowired
    public ManagerSubscriptionController(ClientSubscriptionService clientSubscriptionService, ClientService clientService) {
        this.clientSubscriptionService = clientSubscriptionService;
        this.clientService = clientService;
    }

    @GetMapping("/clientSubscriptions")
    public ClientSubscriptionsResponse getClientSubscriptions() {

        final List<ClientSubscriptionResponse> results = clientSubscriptionService.getClientSubscriptions().stream()
                .map(ClientSubscriptionResponse::new)
                .peek(r -> clientService.getClient(r.getClientId()).ifPresent(c -> {
                    r.setClientName(c.getClientName());
                    r.setClientUsername(c.getUsername());
                }))
                .filter(r -> StringUtils.isNotBlank(r.getClientName()))
                .collect(Collectors.toList());

        return new ClientSubscriptionsResponse(results);
    }

    @GetMapping("/clientSubscriptions/{id}")
    public ClientSubscriptionResponse getClientSubscription(@PathVariable String id) {

        final ClientSubscription clientSubscription = clientSubscriptionService.getClientSubscription(id);

        return new ClientSubscriptionResponse(clientSubscription);
    }

    @PostMapping("/clientSubscriptions/{id}/cancel")
    public ClientSubscriptionResponse cancelClientSubscription(@PathVariable String id) {

        final ClientSubscription clientSubscription = clientSubscriptionService.getClientSubscription(id);
        clientSubscriptionService.cancelClientSubscription(clientSubscription);

        return new ClientSubscriptionResponse(clientSubscription);
    }

    @GetMapping("/clientSubscriptions/{id}/invoices")
    public ClientSubscriptionInvoicesResponse getClientSubscriptionInvoices(@PathVariable String id) {

        final ClientSubscription clientSubscription = clientSubscriptionService.getClientSubscription(id);
        final List<ClientSubscriptionInvoiceResponse> results = clientSubscriptionService.getClientSubscriptionInvoices(clientSubscription).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new ClientSubscriptionInvoicesResponse(results);
    }

    @PostMapping("/invoices/{invoiceIdentifier}/sendInvoice")
    public ClientSubscriptionInvoiceResponse sendClientSubscriptionInvoice(@PathVariable String invoiceIdentifier) {

        final ClientSubscriptionInvoice clientSubscriptionInvoice = clientSubscriptionService.getClientSubscriptionInvoiceByInvoiceIdentifier(invoiceIdentifier);
        Client client = clientService.getClientOrThrows(clientSubscriptionInvoice.getClientSubscription().getClientId());

        clientSubscriptionService.sendClientSubscriptionInvoice(client, clientSubscriptionInvoice);

        return toResponse(clientSubscriptionInvoice);
    }

    @PostMapping("/invoices/{invoiceIdentifier}/activate")
    public ClientSubscriptionInvoiceResponse activateClientSubscription(@PathVariable String invoiceIdentifier) {

        final ClientSubscriptionInvoice paidSubscriptionInvoice = clientSubscriptionService.activateClientSubscription(invoiceIdentifier);

        return toResponse(paidSubscriptionInvoice);
    }

    private ClientSubscriptionInvoiceResponse toResponse(ClientSubscriptionInvoice subscriptionInvoice) {

        return new ClientSubscriptionInvoiceResponse(subscriptionInvoice);
    }
}
