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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ChainedTransaction
class ClientSubscriptionServiceImplTest {

    private final ClientSubscriptionService clientSubscriptionService;

    private final SubscriptionPlanService subscriptionPlanService;

    private final ClientSubscriptionRepository clientSubscriptionRepository;

    private final ClientSubscriptionInvoiceRepository clientSubscriptionInvoiceRepository;

    private final ClientService clientService;

    private final CountrySettings countrySettings;

    private Client client;

    private SubscriptionPlan subscriptionPlan;

    @Autowired
    ClientSubscriptionServiceImplTest(ClientSubscriptionService clientSubscriptionService, SubscriptionPlanService subscriptionPlanService, ClientSubscriptionRepository clientSubscriptionRepository, ClientSubscriptionInvoiceRepository clientSubscriptionInvoiceRepository, ClientService clientService, CountrySettings countrySettings) {
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
    void createAndActivateAndLapseClientSubscription() {

        clientSubscriptionService.createClientSubscription(client, subscriptionPlan.getId(), SubscriptionPlan.PlanPeriod.MONTHLY);
        final ClientSubscriptionInvoice clientSubscriptionInvoice = clientSubscriptionService.createClientSubscription(client, subscriptionPlan.getId(), SubscriptionPlan.PlanPeriod.MONTHLY);

        final ClientSubscription clientSubscription = clientSubscriptionInvoice.getClientSubscription();
        assertThat(clientSubscription.getId()).isNotNull();
        assertThat(clientSubscription.getSubscriptionPlanSnapshot()).isNotNull();
        assertThat(clientSubscription.getStatus()).isEqualByComparingTo(ClientSubscription.SubscriptionStatus.SUBMITTED);
        assertThat(clientSubscription.isCurrent()).isTrue();
        assertThat(clientSubscription.getSubmittedDate()).isNotNull();
        assertThat(clientSubscription.getPlanPrice()).isEqualByComparingTo("490");

        assertThat(clientSubscriptionInvoice).satisfies(inv -> {
            final Date now = new Date();
            assertThat(inv.getValidFrom()).isEqualToIgnoringMinutes(now);

            final Date oneMonthAfter = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).plusMonths(1).toInstant());

            assertThat(inv.getValidTo()).isEqualToIgnoringMinutes(oneMonthAfter);
            assertThat(inv.getDueAmount().getAmount()).isNotZero();
            assertThat(inv.getDueDate()).isEqualTo(inv.getValidFrom());
            assertThat(inv.getSubscriptionPeriod(client.getZoneId())).isNotBlank();
        });

        final ClientSubscriptionInvoice activatedInvoice = clientSubscriptionService.activateClientSubscriptionByInvoiceIdentifier(clientSubscriptionInvoice.getInvoiceIdentifier());

        assertThat(activatedInvoice.getClientSubscription()).satisfies(cs -> {
            assertThat(cs.getStatus()).isEqualByComparingTo(ClientSubscription.SubscriptionStatus.ACTIVE);
            assertThat(cs.getPlanStartDate()).isNotNull();
        });
        assertThat(activatedInvoice.getPaymentDate()).isNotNull();
        assertThat(activatedInvoice.getStatus()).isEqualByComparingTo(ClientSubscriptionInvoice.SubscriptionInvoiceStatus.PAID);

        final ClientSubscription currentClientSubscription = clientSubscriptionService.getCurrentClientSubscription(client.getId());
        clientSubscriptionService.lapseClientSubscription(currentClientSubscription);

        assertThat(currentClientSubscription.getStatus()).isEqualByComparingTo(ClientSubscription.SubscriptionStatus.ACTIVE_LAPSING);
        assertThat(currentClientSubscription.getPlanEndDate()).isNotNull();

        // check immutability of SubscriptionPlanSnapshot
        clientSubscription.getSubscriptionPlanSnapshot().updateSubscriptionLimit(1, 1, List.of("dummyFeature"));
        clientSubscriptionService.saveClientSubscription(clientSubscription);

        assertThat(subscriptionPlanService.getSubscription(subscriptionPlan.getId())).satisfies(s -> assertThat(s.getSubscriptionLimit().getRestrictedFeatures()).isEmpty());
    }

    @Test
    void findSubscriptionInvoicesForRenewal() {

        final ClientSubscription clientSubscription = createClientSubscription();

        createSubscriptionInvoice(clientSubscription, ClientSubscriptionInvoice.SubscriptionInvoiceStatus.PAID);
        createSubscriptionInvoice(clientSubscription, ClientSubscriptionInvoice.SubscriptionInvoiceStatus.PENDING);

        final List<ClientSubscriptionInvoice> newSubscriptionInvoices = clientSubscriptionService.findSubscriptionInvoicesForRenewal();

        assertThat(newSubscriptionInvoices).hasSize(1);
    }

    @Test
    void findUnpaidSubscriptionInvoices() {

        final ClientSubscription clientSubscription = createClientSubscription();

        createSubscriptionInvoice(clientSubscription, ClientSubscriptionInvoice.SubscriptionInvoiceStatus.PENDING);

        final List<ClientSubscriptionInvoice> unpaidSubscriptionInvoices = clientSubscriptionService.findUnpaidSubscriptionInvoices();

        assertThat(unpaidSubscriptionInvoices).hasSize(1);
        assertThat(unpaidSubscriptionInvoices).allSatisfy(inv -> {
            assertThat(inv.getClientSubscription().getStatus()).isEqualByComparingTo(ClientSubscription.SubscriptionStatus.INACTIVE);
            assertThat(inv.getStatus()).isEqualByComparingTo(ClientSubscriptionInvoice.SubscriptionInvoiceStatus.OVERDUE);
        });
    }

    private ClientSubscription createClientSubscription() {

        final ClientSubscription clientSubscription = new ClientSubscription(client.getId(), subscriptionPlan, SubscriptionPlan.PlanPeriod.MONTHLY);
        clientSubscription.setStatus(ClientSubscription.SubscriptionStatus.ACTIVE);
        clientSubscriptionRepository.save(clientSubscription);
        return clientSubscription;
    }

    private void createSubscriptionInvoice(ClientSubscription clientSubscription,
                                           ClientSubscriptionInvoice.SubscriptionInvoiceStatus status) {

        final ClientSubscriptionInvoice subscriptionInvoice = new ClientSubscriptionInvoice(client.getZoneId(), clientSubscription, new Date());
        subscriptionInvoice.setValidTo(Date.from(Instant.now().plus(10, ChronoUnit.DAYS)));
        subscriptionInvoice.setDueDate(new Date());
        subscriptionInvoice.setStatus(status);

        clientSubscriptionInvoiceRepository.save(subscriptionInvoice);
    }
}