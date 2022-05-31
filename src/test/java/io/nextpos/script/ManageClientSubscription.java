package io.nextpos.script;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientRepository;
import io.nextpos.client.service.ClientService;
import io.nextpos.shared.util.DateTimeUtil;
import io.nextpos.subscription.data.*;
import io.nextpos.subscription.service.ClientSubscriptionLifecycleService;
import io.nextpos.subscription.service.ClientSubscriptionOrderService;
import io.nextpos.subscription.service.ClientSubscriptionService;
import io.nextpos.subscription.service.SubscriptionPlanService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Disabled
@SpringBootTest
@ActiveProfiles("gcp")
@TestPropertySource(properties = {"script=true", "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration"})
public class ManageClientSubscription {

    private final SubscriptionPlanService subscriptionPlanService;

    private final ClientSubscriptionService clientSubscriptionService;

    private final ClientSubscriptionOrderService clientSubscriptionOrderService;

    private final ClientSubscriptionLifecycleService clientSubscriptionLifecycleService;

    private final ClientService clientService;

    private final ClientSubscriptionInvoiceRepository clientSubscriptionInvoiceRepository;

    @Autowired
    public ManageClientSubscription(SubscriptionPlanService subscriptionPlanService, ClientSubscriptionService clientSubscriptionService, ClientSubscriptionOrderService clientSubscriptionOrderService, ClientSubscriptionLifecycleService clientSubscriptionLifecycleService, ClientService clientService, ClientSubscriptionInvoiceRepository clientSubscriptionInvoiceRepository) {
        this.subscriptionPlanService = subscriptionPlanService;
        this.clientSubscriptionService = clientSubscriptionService;
        this.clientSubscriptionOrderService = clientSubscriptionOrderService;
        this.clientSubscriptionLifecycleService = clientSubscriptionLifecycleService;
        this.clientService = clientService;
        this.clientSubscriptionInvoiceRepository = clientSubscriptionInvoiceRepository;
    }

    @Test
    void processOrphanedSubscriptionInvoices() {

        Map<String, Client> clients = clientService.getClients().stream()
                .collect(Collectors.toMap(Client::getId, c -> c));

        for (ClientSubscriptionInvoice.SubscriptionInvoiceStatus status : ClientSubscriptionInvoice.SubscriptionInvoiceStatus.values()) {
            final List<ClientSubscriptionInvoice> invoices = clientSubscriptionService.getClientSubscriptionInvoicesByStatuses(List.of(status));
            System.out.printf("%s invoices: %d\n", status, invoices.size());

            if (status == ClientSubscriptionInvoice.SubscriptionInvoiceStatus.PENDING) {
                AtomicInteger count = new AtomicInteger();

                invoices.forEach(inv -> {
                    System.out.printf("Processing invoice id: %s - ",inv.getId());

                    if (inv.getClientSubscription() == null) {
                        count.incrementAndGet();
                        System.out.println("Client subscription reference doesn't exist, deleting invoice...\n");
                        clientSubscriptionInvoiceRepository.delete(inv);

                    } else {
                        final Client client = clients.get(inv.getClientId());
                        System.out.printf("belongs to %s\n", client.getClientName());
                    }
                });

                System.out.printf("Deleted orphaned invoices: %d\n", count.get());
            }
        }
    }

    @Test
    void updateSubscriptionPlan() {

        subscriptionPlanService.getSubscriptionPlans("TW").forEach(sp -> {
            final BigDecimal monthlyPrice = new BigDecimal("1500");
            final BigDecimal yearlyPrice = new BigDecimal("16500");

            sp.addPlanPrice(SubscriptionPlan.PlanPeriod.MONTHLY, new SubscriptionPlan.PlanPrice(monthlyPrice));
            sp.addPlanPrice(SubscriptionPlan.PlanPeriod.YEARLY, new SubscriptionPlan.PlanPrice(yearlyPrice));

            subscriptionPlanService.saveSubscriptionPlan(sp);
        });
    }

    @Test
    void updateClientSubscription() {

        clientService.getClientByUsername("ronandcompanytainan@gmail.com").ifPresent(c -> {
            final ClientSubscription subscription = clientSubscriptionService.getCurrentClientSubscription(c.getId());
            final SubscriptionPlan plan = subscription.getSubscriptionPlanSnapshot();
            final BigDecimal monthlyPrice = new BigDecimal("1500");
            final BigDecimal yearlyPrice = new BigDecimal("13500");

            plan.addPlanPrice(SubscriptionPlan.PlanPeriod.MONTHLY, new SubscriptionPlan.PlanPrice(monthlyPrice));
            plan.addPlanPrice(SubscriptionPlan.PlanPeriod.YEARLY, new SubscriptionPlan.PlanPrice(yearlyPrice));

            subscription.updateSubscriptionPlanPrice(SubscriptionPlan.PlanPeriod.YEARLY, null);

            clientSubscriptionService.saveClientSubscription(subscription);

            System.out.println(subscription);
        });
    }

    @Test
    void displayAllClientSubscriptions() {

        Map<String, Client> clients = clientService.getClients().stream()
                .collect(Collectors.toMap(Client::getId, c -> c));

        List<ClientSubscription> clientSubscriptions = clientSubscriptionService.getClientSubscriptions();
        List<ClientSubscriptionInvoice> invoices = clientSubscriptionInvoiceRepository.findAll().stream()
                .filter(inv -> clients.containsKey(inv.getClientId()))
                .toList();
        List<ClientSubscription> subscriptionsUpForRenewal = clientSubscriptionLifecycleService.findClientSubscriptionsUpForRenewal();
        List<ClientSubscriptionInvoice> unpaidInvoices = clientSubscriptionLifecycleService.findUnpaidSubscriptionInvoices();

        System.out.println("Total clients: " + clients.size());
        System.out.println("Total client subscriptions: " + clientSubscriptions.size());
        System.out.println("Total client subscription invoices: " + invoices.size());
        System.out.println("Total client subscriptions up for renewal this month: " + subscriptionsUpForRenewal.size());

        subscriptionsUpForRenewal.forEach(sub -> {
            System.out.println(clients.get(sub.getClientId()).getClientName());
        });

        System.out.println("Total unpaid subscription invoices: " + unpaidInvoices.size());

        unpaidInvoices.forEach(inv -> {
            Client client = clients.get(inv.getClientId());
            System.out.printf("%s (%s): due amount: %s, due date: %s\n", client.getClientName(), inv.getInvoiceIdentifier(), inv.getDueAmount().getAmount(), inv.getDueDate());
        });
    }

    @Test
    void createClientRenewalInvoices() {

        final List<ClientSubscriptionInvoice> invoices = clientSubscriptionLifecycleService.renewActiveClientSubscriptions();

        System.out.println("Created subscription invoices: " + invoices.size());
    }

    @Test
    void getClientSubscription() {

        clientService.getClientByUsername("Stancwm@gmail.com").ifPresent(c -> {
            final ClientSubscription subscription = clientSubscriptionService.getCurrentClientSubscription(c.getId());
            final List<ClientSubscriptionInvoice> invoices = clientSubscriptionService.getClientSubscriptionInvoices(subscription);

            System.out.println("Client subscription id: " + subscription.getId());
            System.out.println("Client subscription status: " + subscription.getStatus());

            invoices.forEach(inv -> {
                System.out.printf("Invoice %s (%s) %s %s: %s%n", inv.getId(), inv.getInvoiceIdentifier(), inv.getValidFrom(), inv.getValidTo(), inv.getStatus());
            });
        });
    }

    @Test
    void renewClientSubscriptionForSpecificDate() {

        final SubscriptionPlan.PlanPeriod newPlanPeriod = SubscriptionPlan.PlanPeriod.YEARLY;

        clientService.getClientByUsername("ronandcompanytainan@gmail.com").ifPresent(c -> {
            final ClientSubscription subscription = clientSubscriptionService.getCurrentClientSubscription(c.getId());

            if (newPlanPeriod != subscription.getPlanPeriod()) {
                subscription.updateSubscriptionPlanPrice(newPlanPeriod, BigDecimal.ZERO);
                clientSubscriptionService.saveClientSubscription(subscription);
                System.out.println("Updated client subscription plan: " + subscription);
            }

            Date renewalDate = DateTimeUtil.toDate(c.getZoneId(), LocalDateTime.of(2021, 11, 1, 0, 0, 0));
            final ClientSubscriptionInvoice renewalInvoice = clientSubscriptionService.createAndSendClientSubscriptionInvoice(c, subscription, renewalDate, true, false);

            System.out.println("Renewal invoice identifier: " + renewalInvoice.getInvoiceIdentifier());
            System.out.println("Created renewal invoice: " + renewalInvoice);
        });
    }

    @Test
    void sendClientSubscriptionInvoice() {
        clientService.getClientByUsername("Stancwm@gmail.com").ifPresent(c -> {
            final ClientSubscription subscription = clientSubscriptionService.getCurrentClientSubscription(c.getId());
            final ClientSubscriptionInvoice invoice = clientSubscriptionService.getClientSubscriptionInvoice(subscription.getCurrentInvoiceId());

            clientSubscriptionService.sendClientSubscriptionInvoice(c, invoice);
        });
    }

    @Test
    void activateClientSubscription() {

        String invoiceIdentifier = "798674";
        clientService.getClientByUsername("Stancwm@gmail.com").ifPresent(c -> {
            final ClientSubscriptionInvoice paid = clientSubscriptionService.activateClientSubscriptionByInvoiceIdentifier(invoiceIdentifier, true);
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
