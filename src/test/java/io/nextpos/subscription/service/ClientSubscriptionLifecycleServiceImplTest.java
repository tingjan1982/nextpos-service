package io.nextpos.subscription.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.settings.data.CountrySettings;
import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import io.nextpos.subscription.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ChainedTransaction
class ClientSubscriptionLifecycleServiceImplTest {

    private final ClientSubscriptionLifecycleService clientSubscriptionLifecycleService;

    private final ClientSubscriptionService clientSubscriptionService;

    private final SubscriptionPlanService subscriptionPlanService;

    private final ClientSubscriptionRepository clientSubscriptionRepository;

    private final ClientSubscriptionInvoiceRepository clientSubscriptionInvoiceRepository;

    private final ClientService clientService;

    private final CountrySettings countrySettings;

    private Client client;

    private SubscriptionPlan subscriptionPlan;

    @Autowired
    ClientSubscriptionLifecycleServiceImplTest(ClientSubscriptionLifecycleService clientSubscriptionLifecycleService, ClientSubscriptionService clientSubscriptionService, SubscriptionPlanService subscriptionPlanService, ClientSubscriptionRepository clientSubscriptionRepository, ClientSubscriptionInvoiceRepository clientSubscriptionInvoiceRepository, ClientService clientService, CountrySettings countrySettings) {
        this.clientSubscriptionLifecycleService = clientSubscriptionLifecycleService;
        this.clientSubscriptionService = clientSubscriptionService;
        this.subscriptionPlanService = subscriptionPlanService;
        this.clientSubscriptionRepository = clientSubscriptionRepository;
        this.clientSubscriptionInvoiceRepository = clientSubscriptionInvoiceRepository;
        this.clientService = clientService;
        this.countrySettings = countrySettings;
    }

    @BeforeEach
    void prepare() {
        client = DummyObjects.dummyClient();
        clientService.saveClient(client);

        subscriptionPlan = new SubscriptionPlan(Locale.TAIWAN.getCountry(), SubscriptionPlan.PlanGroup.DEFAULT, "standard", countrySettings);
        subscriptionPlan.addPlanPrice(SubscriptionPlan.PlanPeriod.MONTHLY, new SubscriptionPlan.PlanPrice(new BigDecimal("490")));
        subscriptionPlanService.saveSubscriptionPlan(subscriptionPlan);

        final SubscriptionPaymentInstruction instruction = new SubscriptionPaymentInstruction(Locale.TAIWAN.getCountry(), "d-dd8bd80c86c74ea9a9ff2a96dcfb462d");
        subscriptionPlanService.saveSubscriptionPaymentInstruction(instruction);
    }

    @Test
    void findSubscriptionInvoicesForRenewal() {

        final ClientSubscription clientSubscription = createClientSubscription(ClientSubscription.SubscriptionStatus.ACTIVE);

        createSubscriptionInvoice(clientSubscription, ClientSubscriptionInvoice.SubscriptionInvoiceStatus.PAID);

        final List<ClientSubscriptionInvoice> renewalInvoices = clientSubscriptionLifecycleService.findSubscriptionInvoicesForRenewal();

        assertThat(renewalInvoices).hasSize(1);
        assertThat(clientSubscriptionLifecycleService.findSubscriptionInvoicesForRenewal()).isEmpty();

        final ClientSubscriptionInvoice renewalInvoice = renewalInvoices.get(0);

        assertThat(clientSubscriptionService.getClientSubscriptionInvoices(clientSubscription)).hasSize(2);

        assertThat(clientSubscriptionService.getClientSubscription(clientSubscription.getId())).satisfies(sub -> {
            assertThat(sub.getStatus()).isEqualByComparingTo(ClientSubscription.SubscriptionStatus.ACTIVE_RENEWING);
            assertThat(sub.getCurrentInvoiceId()).isEqualTo(renewalInvoice.getId());
        });

        final ClientSubscriptionInvoice renewedInvoice = clientSubscriptionService.activateClientSubscriptionByInvoiceIdentifier(renewalInvoice.getInvoiceIdentifier(), true);

        assertThat(renewedInvoice.getStatus()).isEqualByComparingTo(ClientSubscriptionInvoice.SubscriptionInvoiceStatus.PAID);
    }

    @Test
    void findUnpaidSubscriptionInvoices() {

        final ClientSubscription clientSubscription = createClientSubscription(ClientSubscription.SubscriptionStatus.ACTIVE);

        createSubscriptionInvoice(clientSubscription, ClientSubscriptionInvoice.SubscriptionInvoiceStatus.PENDING);

        final List<ClientSubscriptionInvoice> unpaidSubscriptionInvoices = clientSubscriptionLifecycleService.findUnpaidSubscriptionInvoices();

        assertThat(unpaidSubscriptionInvoices).hasSize(1);
        assertThat(unpaidSubscriptionInvoices).allSatisfy(inv -> {
            assertThat(inv.getClientSubscription().getStatus()).isEqualByComparingTo(ClientSubscription.SubscriptionStatus.ACTIVE); // unchanged
            assertThat(inv.getStatus()).isEqualByComparingTo(ClientSubscriptionInvoice.SubscriptionInvoiceStatus.OVERDUE);
        });

        final List<ClientSubscriptionInvoice> outstandingInvoices = clientSubscriptionService.getClientSubscriptionInvoicesByStatuses(
                List.of(ClientSubscriptionInvoice.SubscriptionInvoiceStatus.PENDING, ClientSubscriptionInvoice.SubscriptionInvoiceStatus.OVERDUE));

        assertThat(outstandingInvoices).hasSize(1);
    }

    @Test
    void processActiveLapsingClientSubscriptions() {

        final ClientSubscription clientSubscription = createClientSubscription(ClientSubscription.SubscriptionStatus.ACTIVE_LAPSING);
        clientSubscription.setPlanEndDate(new Date());
        clientSubscriptionRepository.save(clientSubscription);
        createSubscriptionInvoice(clientSubscription, ClientSubscriptionInvoice.SubscriptionInvoiceStatus.PAID);
        
        final List<ClientSubscription> clientSubscriptions = clientSubscriptionLifecycleService.processActiveLapsingClientSubscriptions();

        assertThat(clientSubscriptions).isNotEmpty();
        assertThat(clientSubscriptions).allSatisfy(cs -> {
            assertThat(cs.getStatus()).isEqualByComparingTo(ClientSubscription.SubscriptionStatus.LAPSED);
        });
    }

    private ClientSubscription createClientSubscription(ClientSubscription.SubscriptionStatus status) {

        final ClientSubscription clientSubscription = new ClientSubscription(client.getId(), subscriptionPlan, SubscriptionPlan.PlanPeriod.MONTHLY);
        clientSubscription.setStatus(status);
        clientSubscription.setPlanStartDate(new Date());
        clientSubscription.setPlanEndDate(Date.from(Instant.now().plus(10, ChronoUnit.DAYS)));

        return clientSubscriptionRepository.save(clientSubscription);
    }

    private void createSubscriptionInvoice(ClientSubscription clientSubscription,
                                           ClientSubscriptionInvoice.SubscriptionInvoiceStatus status) {

        final ClientSubscriptionInvoice invoice = new ClientSubscriptionInvoice(client.getZoneId(), clientSubscription, new Date());
        invoice.setValidTo(Date.from(Instant.now().plus(10, ChronoUnit.DAYS)));
        invoice.setDueDate(new Date());
        invoice.setStatus(status);

        clientSubscriptionInvoiceRepository.save(invoice);
        clientSubscription.setCurrentInvoiceId(invoice.getId());
        clientSubscriptionRepository.save(clientSubscription);
    }
}