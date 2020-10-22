package io.nextpos.subscription.web;

import io.nextpos.subscription.data.ClientSubscriptionInvoice;
import io.nextpos.subscription.service.ClientSubscriptionService;
import io.nextpos.subscription.web.model.ActivateSubscriptionRequest;
import io.nextpos.subscription.web.model.ClientSubscriptionInvoiceResponse;
import io.nextpos.subscription.web.model.ClientSubscriptionInvoicesResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/clientSubscriptions")
public class ManagerSubscriptionController {

    private final ClientSubscriptionService clientSubscriptionService;

    @Autowired
    public ManagerSubscriptionController(ClientSubscriptionService clientSubscriptionService) {
        this.clientSubscriptionService = clientSubscriptionService;
    }

    @PostMapping("/activate")
    public ClientSubscriptionInvoiceResponse activateClientSubscription(@RequestBody ActivateSubscriptionRequest request) {

        final ClientSubscriptionInvoice paidSubscriptionInvoice = clientSubscriptionService.activateClientSubscription(request.getInvoiceIdentifier());

        return toResponse(paidSubscriptionInvoice);
    }

    @GetMapping("/pending")
    public ClientSubscriptionInvoicesResponse getPendingSubscriptionInvoices() {

        final List<ClientSubscriptionInvoiceResponse> results = clientSubscriptionService.getClientSubscriptionInvoicesByStatus(ClientSubscriptionInvoice.SubscriptionInvoiceStatus.PENDING).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new ClientSubscriptionInvoicesResponse(results);
    }

    @GetMapping("/{id}")
    public ClientSubscriptionInvoiceResponse getClientSubscription(@PathVariable String id) {

        final ClientSubscriptionInvoice paidSubscriptionInvoice = clientSubscriptionService.getClientSubscriptionInvoice(id);

        return toResponse(paidSubscriptionInvoice);
    }

    private ClientSubscriptionInvoiceResponse toResponse(ClientSubscriptionInvoice subscriptionInvoice) {

        return new ClientSubscriptionInvoiceResponse(subscriptionInvoice.getId(),
                subscriptionInvoice.getInvoiceIdentifier(),
                subscriptionInvoice.getStatus(),
                subscriptionInvoice.getValidFrom(),
                subscriptionInvoice.getValidTo(),
                subscriptionInvoice.getPaymentDate());
    }
}
