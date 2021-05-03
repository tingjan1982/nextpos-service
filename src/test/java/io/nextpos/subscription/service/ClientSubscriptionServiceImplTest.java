package io.nextpos.subscription.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.settings.data.CountrySettings;
import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import io.nextpos.shared.util.DateTimeUtil;
import io.nextpos.subscription.data.*;
import io.nextpos.subscription.service.bean.CreateClientSubscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

    private final ClientService clientService;

    private final CountrySettings countrySettings;

    private Client client;

    private SubscriptionPlan subscriptionPlan;

    private SubscriptionPlan subscriptionPlan2;

    @Autowired
    ClientSubscriptionServiceImplTest(ClientSubscriptionService clientSubscriptionService, SubscriptionPlanService subscriptionPlanService, ClientSubscriptionRepository clientSubscriptionRepository, ClientService clientService, CountrySettings countrySettings) {
        this.clientSubscriptionService = clientSubscriptionService;
        this.subscriptionPlanService = subscriptionPlanService;
        this.clientSubscriptionRepository = clientSubscriptionRepository;
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

        subscriptionPlan2 = new SubscriptionPlan(Locale.TAIWAN.getCountry(), SubscriptionPlan.PlanGroup.DEFAULT, "premium", countrySettings);
        subscriptionPlan2.addPlanPrice(SubscriptionPlan.PlanPeriod.MONTHLY, new SubscriptionPlan.PlanPrice(new BigDecimal("990")));
        subscriptionPlanService.saveSubscriptionPlan(subscriptionPlan2);

        final SubscriptionPaymentInstruction instruction = new SubscriptionPaymentInstruction(Locale.TAIWAN.getCountry(), "d-dd8bd80c86c74ea9a9ff2a96dcfb462d");
        subscriptionPlanService.saveSubscriptionPaymentInstruction(instruction);
    }

    @Test
    void clientSubscriptionLifecycle() {

        final CreateClientSubscription createClientSubscription = new CreateClientSubscription(subscriptionPlan.getId(), SubscriptionPlan.PlanPeriod.MONTHLY, BigDecimal.ZERO);
        clientSubscriptionService.createClientSubscription(client, createClientSubscription);
        final ClientSubscriptionInvoice clientSubscriptionInvoice = clientSubscriptionService.createClientSubscription(client, createClientSubscription);

        final ClientSubscription clientSubscription = clientSubscriptionInvoice.getClientSubscription();
        assertThat(clientSubscription.getCurrentInvoiceId()).isEqualTo(clientSubscriptionInvoice.getId());
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

        final ClientSubscriptionInvoice activatedInvoice = clientSubscriptionService.activateClientSubscriptionByInvoiceIdentifier(clientSubscriptionInvoice.getInvoiceIdentifier(), true);
        assertThat(activatedInvoice.getPaymentDate()).isNotNull();
        assertThat(activatedInvoice.getStatus()).isEqualByComparingTo(ClientSubscriptionInvoice.SubscriptionInvoiceStatus.PAID);

        assertThat(activatedInvoice.getClientSubscription()).satisfies(cs -> {
            assertThat(cs.getStatus()).isEqualByComparingTo(ClientSubscription.SubscriptionStatus.ACTIVE);
            assertThat(cs.getPlanStartDate()).isNotNull();
            assertThat(cs.getPlanEndDate()).isNotNull();
        });

        // decide to sign up after 2 weeks
        final Date newPlanStartDate = DateTimeUtil.toDate(client.getZoneId(), LocalDateTime.now().plusDays(14));
        final ClientSubscriptionInvoice newInvoice = clientSubscriptionService.createClientSubscriptionInvoice(client, clientSubscription, newPlanStartDate);

        assertThat(newInvoice.getStatus()).isEqualByComparingTo(ClientSubscriptionInvoice.SubscriptionInvoiceStatus.PENDING);
        assertThat(newInvoice.getValidFrom()).isEqualTo(newPlanStartDate);
        assertThat(newInvoice.getPaymentDate()).isNull();

        assertThat(clientSubscriptionService.activateClientSubscriptionByInvoiceIdentifier(newInvoice.getInvoiceIdentifier(), true)).satisfies(inv -> {
            assertThat(inv.getPaymentDate()).isEqualToIgnoringSeconds(new Date());
            assertThat(inv.getStatus()).isEqualByComparingTo(ClientSubscriptionInvoice.SubscriptionInvoiceStatus.PAID);

            assertThat(inv.getClientSubscription()).satisfies(cs -> {
                assertThat(cs.getStatus()).isEqualByComparingTo(ClientSubscription.SubscriptionStatus.ACTIVE);
                assertThat(cs.getPlanStartDate()).isEqualToIgnoringSeconds(newPlanStartDate);
                assertThat(cs.getPlanEndDate()).isEqualToIgnoringSeconds(newInvoice.getValidTo());
            });
        });

        // ===== upgrade plan scenario =====
        final ClientSubscriptionInvoice newPlanInvoice = clientSubscriptionService.createClientSubscription(client, new CreateClientSubscription(subscriptionPlan2.getId(), SubscriptionPlan.PlanPeriod.MONTHLY, BigDecimal.ZERO));

        assertThat(newPlanInvoice.getClientSubscription().isCurrent()).isFalse();
        assertThat(clientSubscriptionService.getCurrentClientSubscription(client.getId())).satisfies(sub -> {
            assertThat(sub.getId()).isEqualTo(clientSubscription.getId());
        });

        final ClientSubscriptionInvoice paidInvoice = clientSubscriptionService.activateClientSubscriptionByInvoiceIdentifier(newPlanInvoice.getInvoiceIdentifier(), true);
        assertThat(paidInvoice.getStatus()).isEqualByComparingTo(ClientSubscriptionInvoice.SubscriptionInvoiceStatus.PAID);
        assertThat(clientSubscriptionService.getClientSubscription(clientSubscription.getId())).satisfies(sub -> {
            assertThat(sub.getStatus()).isEqualByComparingTo(ClientSubscription.SubscriptionStatus.LAPSED);
            assertThat(sub.isCurrent()).isFalse();
        });

        final ClientSubscription currentClientSubscription = clientSubscriptionService.getCurrentClientSubscription(client.getId());
        assertThat(currentClientSubscription.getId()).isEqualTo(paidInvoice.getClientSubscription().getId());

        clientSubscriptionService.lapseClientSubscription(currentClientSubscription);

        assertThat(currentClientSubscription.getStatus()).isEqualByComparingTo(ClientSubscription.SubscriptionStatus.ACTIVE_LAPSING);

        clientSubscriptionService.deactivateClientSubscription(currentClientSubscription);

        assertThat(currentClientSubscription.getStatus()).isEqualByComparingTo(ClientSubscription.SubscriptionStatus.INACTIVE);

        clientSubscriptionService.cancelClientSubscription(currentClientSubscription);

        assertThat(currentClientSubscription.getStatus()).isEqualByComparingTo(ClientSubscription.SubscriptionStatus.CANCELLED);

        // check immutability of SubscriptionPlanSnapshot
        clientSubscription.getSubscriptionPlanSnapshot().updateSubscriptionLimit(1, 1, List.of("dummyFeature"));
        clientSubscriptionRepository.save(clientSubscription);

        assertThat(subscriptionPlanService.getSubscription(subscriptionPlan.getId())).satisfies(s -> assertThat(s.getSubscriptionLimit().getRestrictedFeatures()).isEmpty());
    }
}