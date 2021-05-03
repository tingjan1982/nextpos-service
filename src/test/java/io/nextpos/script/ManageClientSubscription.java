package io.nextpos.script;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.shared.util.DateTimeUtil;
import io.nextpos.subscription.data.ClientSubscription;
import io.nextpos.subscription.data.ClientSubscriptionInvoice;
import io.nextpos.subscription.data.ClientSubscriptionInvoiceRepository;
import io.nextpos.subscription.data.SubscriptionPlan;
import io.nextpos.subscription.service.ClientSubscriptionOrderService;
import io.nextpos.subscription.service.ClientSubscriptionService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

    private final ClientService clientService;

    private final ClientSubscriptionInvoiceRepository clientSubscriptionInvoiceRepository;

    @Autowired
    public ManageClientSubscription(ClientSubscriptionService clientSubscriptionService, ClientSubscriptionOrderService clientSubscriptionOrderService, ClientService clientService, ClientSubscriptionInvoiceRepository clientSubscriptionInvoiceRepository) {
        this.clientSubscriptionService = clientSubscriptionService;
        this.clientSubscriptionOrderService = clientSubscriptionOrderService;
        this.clientService = clientService;
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
    void updateSubscription() {

        clientService.getClientByUsername("a0919910@gmail.com").ifPresent(c -> {
            final ClientSubscription subscription = clientSubscriptionService.getCurrentClientSubscription(c.getId());
            subscription.setPlanPeriod(SubscriptionPlan.PlanPeriod.HALF_YEARLY);
            clientSubscriptionService.saveClientSubscription(subscription);

            System.out.println("Updated client subscription: " + subscription);

            Date renewalDate = DateTimeUtil.toDate(c.getZoneId(), LocalDateTime.of(2021, 5, 1, 0, 0, 0));
            final ClientSubscriptionInvoice renewalInvoice = new ClientSubscriptionInvoice(c.getZoneId(), subscription, renewalDate, true);
            clientSubscriptionInvoiceRepository.save(renewalInvoice);

            System.out.println("Updated renewal invoice: " + renewalInvoice);

            subscription.setCurrentInvoiceId(renewalInvoice.getId());
            clientSubscriptionService.saveClientSubscription(subscription);

            System.out.println("Updated client subscription: " + subscription);

            final ClientSubscriptionInvoice paid = clientSubscriptionService.activateClientSubscriptionByInvoiceIdentifier(renewalInvoice.getInvoiceIdentifier(), false);

            System.out.println("Updated invoice: " + paid);
        });
    }

    @Test
    void sendSubscriptionInvoice() {
        clientService.getClientByUsername("a0919910@gmail.com").ifPresent(c -> {
            final ClientSubscription subscription = clientSubscriptionService.getCurrentClientSubscription(c.getId());
            final ClientSubscriptionInvoice invoice = clientSubscriptionService.getClientSubscriptionInvoice(subscription.getCurrentInvoiceId());

            clientSubscriptionOrderService.sendClientSubscriptionOrder(invoice, "rain.io.app@gmail.com");
        });
    }
}
