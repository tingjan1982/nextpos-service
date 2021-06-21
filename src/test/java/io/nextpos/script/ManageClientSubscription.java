package io.nextpos.script;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientRepository;
import io.nextpos.client.service.ClientService;
import io.nextpos.shared.util.DateTimeUtil;
import io.nextpos.subscription.data.ClientSubscription;
import io.nextpos.subscription.data.ClientSubscriptionInvoice;
import io.nextpos.subscription.data.ClientSubscriptionInvoiceRepository;
import io.nextpos.subscription.data.SubscriptionPlan;
import io.nextpos.subscription.service.ClientSubscriptionLifecycleService;
import io.nextpos.subscription.service.ClientSubscriptionOrderService;
import io.nextpos.subscription.service.ClientSubscriptionService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Disabled
@SpringBootTest
@ActiveProfiles("gcp")
@TestPropertySource(properties = {"script=true", "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration"})
public class ManageClientSubscription {

    private final ClientSubscriptionService clientSubscriptionService;

    private final ClientSubscriptionOrderService clientSubscriptionOrderService;

    private final ClientSubscriptionLifecycleService clientSubscriptionLifecycleService;

    private final ClientService clientService;

    private final ClientRepository clientRepository;

    private final ClientSubscriptionInvoiceRepository clientSubscriptionInvoiceRepository;

    @Autowired
    public ManageClientSubscription(ClientSubscriptionService clientSubscriptionService, ClientSubscriptionOrderService clientSubscriptionOrderService, ClientSubscriptionLifecycleService clientSubscriptionLifecycleService, ClientService clientService, ClientRepository clientRepository, ClientSubscriptionInvoiceRepository clientSubscriptionInvoiceRepository) {
        this.clientSubscriptionService = clientSubscriptionService;
        this.clientSubscriptionOrderService = clientSubscriptionOrderService;
        this.clientSubscriptionLifecycleService = clientSubscriptionLifecycleService;
        this.clientService = clientService;
        this.clientRepository = clientRepository;
        this.clientSubscriptionInvoiceRepository = clientSubscriptionInvoiceRepository;
    }

    @Test
    void checkSubscriptionStatuses() {

        for (ClientSubscriptionInvoice.SubscriptionInvoiceStatus status : ClientSubscriptionInvoice.SubscriptionInvoiceStatus.values()) {
            final List<ClientSubscriptionInvoice> invoices = clientSubscriptionService.getClientSubscriptionInvoicesByStatuses(List.of(status));
            System.out.println("Invoice by status: " + status + "=" + invoices.size());

            if (status == ClientSubscriptionInvoice.SubscriptionInvoiceStatus.PENDING) {
                invoices.forEach(inv -> {
                    System.out.println("invoice id: " + inv.getId());

                    if (inv.getClientSubscription() == null) {
                        System.out.println("sub doesn't exist");
                        clientSubscriptionInvoiceRepository.delete(inv);

                    } else {
                        final Client client = clientService.getClientOrThrows(inv.getClientSubscription().getClientId());
                        System.out.println(client.getClientName());
                    }
                });
            }
        }
    }

    @Test
    void getClientSubscriptionsUpFoRenewal() {

        final List<ClientSubscription> subscriptions = clientSubscriptionLifecycleService.findClientSubscriptionsUpForRenewal();

        System.out.println("Subscriptions: " + subscriptions.size());
    }

    @Test
    void createClientRenewalInvoices() {

        final List<ClientSubscriptionInvoice> invoices = clientSubscriptionLifecycleService.findSubscriptionInvoicesForRenewal();

        System.out.println("Created subscription invoices: " + invoices.size());
    }

    @Test
    void getClientSubscription() {

        clientService.getClientByUsername("Stancwm@gmail.com").ifPresent(c -> {
            final ClientSubscription subscription = clientSubscriptionService.getCurrentClientSubscription(c.getId());
            final List<ClientSubscriptionInvoice> invoices = clientSubscriptionService.getClientSubscriptionInvoices(subscription);

            System.out.println(subscription);
            System.out.println(invoices.size());
        });
    }

    @Test
    void renewClientSubscription() {

        final SubscriptionPlan.PlanPeriod newPlanPeriod = SubscriptionPlan.PlanPeriod.MONTHLY;

        clientService.getClientByUsername("Stancwm@gmail.com").ifPresent(c -> {
            final ClientSubscription subscription = clientSubscriptionService.getCurrentClientSubscription(c.getId());

            if (newPlanPeriod != subscription.getPlanPeriod()) {
                subscription.setPlanPeriod(newPlanPeriod);
                clientSubscriptionService.saveClientSubscription(subscription);

                System.out.println("Updated client subscription plan period: " + subscription);
            }

            Date renewalDate = DateTimeUtil.toDate(c.getZoneId(), LocalDateTime.of(2021, 6, 1, 0, 0, 0));
            final ClientSubscriptionInvoice renewalInvoice = clientSubscriptionService.createClientSubscriptionInvoice(c, subscription, renewalDate);

            System.out.println("Renewal invoice identifier: " + renewalInvoice.getInvoiceIdentifier());
            System.out.println("Created renewal invoice: " + renewalInvoice);
        });
    }

    @Test
    void activateClientSubscription() {

        String invoiceIdentifier = "419800";
        clientService.getClientByUsername("Stancwm@gmail.com").ifPresent(c -> {
            final ClientSubscriptionInvoice paid = clientSubscriptionService.activateClientSubscriptionByInvoiceIdentifier(invoiceIdentifier, false);
            System.out.println("Paid invoice: " + paid);
        });
    }

    @Test
    @WithMockUser("rain.io.app@gmail.com")
    void sendSubscriptionInvoice() {
        clientService.getClientByUsername("Stancwm@gmail.com").ifPresent(c -> {
            final ClientSubscription subscription = clientSubscriptionService.getCurrentClientSubscription(c.getId());
            final ClientSubscriptionInvoice invoice = clientSubscriptionService.getClientSubscriptionInvoice(subscription.getCurrentInvoiceId());

            clientSubscriptionOrderService.sendClientSubscriptionOrder(invoice, null);
        });
    }
}
