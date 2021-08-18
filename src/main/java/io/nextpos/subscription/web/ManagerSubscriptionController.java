package io.nextpos.subscription.web;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.shared.util.DateTimeUtil;
import io.nextpos.subscription.data.ClientSubscription;
import io.nextpos.subscription.data.ClientSubscriptionInvoice;
import io.nextpos.subscription.service.ClientSubscriptionService;
import io.nextpos.subscription.web.model.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
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

    @PostMapping("/clientSubscriptions")
    public ClientSubscriptionInvoiceResponse submitClientSubscription(@Valid @RequestBody ClientSubscriptionRequest request) {

        final Client client = clientService.getClientOrThrows(request.getClientId());
        final ClientSubscriptionInvoice invoice = clientSubscriptionService.createClientSubscription(client, request.toCreateClientSubscription());

        return new ClientSubscriptionInvoiceResponse(invoice);
    }

    @GetMapping("/clientSubscriptions/{id}")
    public ClientSubscriptionResponse getClientSubscription(@PathVariable String id) {

        final ClientSubscription clientSubscription = clientSubscriptionService.getClientSubscription(id);

        return new ClientSubscriptionResponse(clientSubscription);
    }

    @PostMapping("/clientSubscriptions/{id}/features")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void toggleClientSubscriptionFeatures(@PathVariable String id, @RequestBody FeaturesRequest request) {

        final ClientSubscription clientSubscription = clientSubscriptionService.getClientSubscription(id);
        clientSubscription.updateSubscriptionFeature(request.getFeature(), request.isEnabled());

        clientSubscriptionService.saveClientSubscription(clientSubscription);
    }

    @PostMapping("/clientSubscriptions/{id}/renew")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void renewClientSubscription(@PathVariable String id, @RequestBody RenewSubscriptionRequest request) {

        final ClientSubscription clientSubscription = clientSubscriptionService.getClientSubscription(id);
        clientService.getClient(clientSubscription.getClientId()).ifPresent(c -> {
            Date renewalDate = clientSubscription.getPlanEndDate();

            if (request.getRenewalDate() != null) {
                renewalDate = DateTimeUtil.toDate(c.getZoneId(), LocalDateTime.of(request.getRenewalDate(), LocalTime.of(0, 0)));
            }

            clientSubscriptionService.createClientSubscriptionInvoice(c, clientSubscription, renewalDate);
        });
    }

    @PostMapping("/clientSubscriptions/{id}/lapse")
    public ClientSubscriptionResponse lapseClientSubscription(@PathVariable String id) {

        final ClientSubscription clientSubscription = clientSubscriptionService.getClientSubscription(id);
        clientSubscriptionService.lapseClientSubscription(clientSubscription);

        return new ClientSubscriptionResponse(clientSubscription);
    }

    @PostMapping("/clientSubscriptions/{id}/deactivate")
    public ClientSubscriptionResponse deactivateClientSubscription(@PathVariable String id) {

        final ClientSubscription clientSubscription = clientSubscriptionService.getClientSubscription(id);
        clientSubscriptionService.deactivateClientSubscription(clientSubscription);

        return new ClientSubscriptionResponse(clientSubscription);
    }

    @PostMapping("/clientSubscriptions/{id}/reactivate")
    public ClientSubscriptionResponse reactivateClientSubscription(@PathVariable String id) {

        final ClientSubscription clientSubscription = clientSubscriptionService.getClientSubscription(id);
        clientSubscriptionService.activateClientSubscription(clientSubscription);

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

    @GetMapping("/invoices/outstanding")
    public ClientSubscriptionInvoicesResponse getOutstandingClientInvoices() {

        final List<ClientSubscriptionInvoiceResponse> invoices = clientSubscriptionService.getClientSubscriptionInvoicesByStatuses(
                        List.of(ClientSubscriptionInvoice.SubscriptionInvoiceStatus.PENDING, ClientSubscriptionInvoice.SubscriptionInvoiceStatus.OVERDUE)).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new ClientSubscriptionInvoicesResponse(invoices);

    }

    @PostMapping("/invoices/{id}/sendInvoice")
    public ClientSubscriptionInvoiceResponse sendClientSubscriptionInvoice(@PathVariable String id) {

        final ClientSubscriptionInvoice clientSubscriptionInvoice = clientSubscriptionService.getClientSubscriptionInvoice(id);
        Client client = clientService.getClientOrThrows(clientSubscriptionInvoice.getClientSubscription().getClientId());

        clientSubscriptionService.sendClientSubscriptionInvoice(client, clientSubscriptionInvoice);

        return toResponse(clientSubscriptionInvoice);
    }

    @PostMapping("/invoices/{invoiceIdentifier}/activate")
    public ClientSubscriptionInvoiceResponse activateClientSubscription(@PathVariable String invoiceIdentifier) {

        final ClientSubscriptionInvoice paidSubscriptionInvoice = clientSubscriptionService.activateClientSubscriptionByInvoiceIdentifier(invoiceIdentifier, false);

        return toResponse(paidSubscriptionInvoice);
    }

    private ClientSubscriptionInvoiceResponse toResponse(ClientSubscriptionInvoice subscriptionInvoice) {

        return new ClientSubscriptionInvoiceResponse(subscriptionInvoice);
    }

    @DeleteMapping("/invoices/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteClientSubscriptionInvoice(@PathVariable String id) {

        clientSubscriptionService.deleteClientSubscriptionInvoice(id);
    }
}
