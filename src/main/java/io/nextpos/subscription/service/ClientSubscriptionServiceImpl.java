package io.nextpos.subscription.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.notification.data.DynamicEmailDetails;
import io.nextpos.notification.service.NotificationService;
import io.nextpos.settings.data.CountrySettings;
import io.nextpos.settings.service.SettingsService;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import io.nextpos.subscription.data.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@ChainedTransaction
public class ClientSubscriptionServiceImpl implements ClientSubscriptionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSubscriptionServiceImpl.class);

    private final SubscriptionPlanService subscriptionPlanService;

    private final NotificationService notificationService;

    private final ClientService clientService;

    private final SettingsService settingsService;

    private final ClientSubscriptionRepository clientSubscriptionRepository;

    private final ClientSubscriptionInvoiceRepository clientSubscriptionInvoiceRepository;

    @Autowired
    public ClientSubscriptionServiceImpl(SubscriptionPlanService subscriptionPlanService, NotificationService notificationService, ClientService clientService, SettingsService settingsService, ClientSubscriptionRepository clientSubscriptionRepository, ClientSubscriptionInvoiceRepository clientSubscriptionInvoiceRepository) {
        this.subscriptionPlanService = subscriptionPlanService;
        this.notificationService = notificationService;
        this.clientService = clientService;
        this.settingsService = settingsService;
        this.clientSubscriptionRepository = clientSubscriptionRepository;
        this.clientSubscriptionInvoiceRepository = clientSubscriptionInvoiceRepository;
    }

    @Override
    public ClientSubscription createClientSubscription(Client client, String subscriptionPlanId, SubscriptionPlan.PlanPeriod planPeriod) {

        SubscriptionPlan subscriptionPlanSnapshot = subscriptionPlanService.getSubscription(subscriptionPlanId);
        final ClientSubscription clientSubscription = new ClientSubscription(client.getId(), subscriptionPlanSnapshot, planPeriod);
        this.saveClientSubscription(clientSubscription);

        createAndSendClientSubscriptionInvoice(client, clientSubscription, new Date());

        return clientSubscription;
    }

    private ClientSubscriptionInvoice createAndSendClientSubscriptionInvoice(Client client, ClientSubscription clientSubscription, Date subscriptionValidFrom) {

        final ClientSubscriptionInvoice subscriptionInvoice = new ClientSubscriptionInvoice(client.getZoneId(), clientSubscription, subscriptionValidFrom);

        final SubscriptionPlan subscriptionPlanSnapshot = clientSubscription.getSubscriptionPlanSnapshot();
        final SubscriptionPaymentInstruction instruction = subscriptionPlanService.getSubscriptionPaymentInstructionByCountryOrThrows(subscriptionPlanSnapshot.getCountryCode());

        final CountrySettings countrySettings = settingsService.getCountrySettings(client.getCountryCode());
        final CountrySettings.RoundingAmountHelper helper = countrySettings.roundingAmountHelper();

        final DynamicEmailDetails dynamicEmailDetails = new DynamicEmailDetails(client.getId(), client.getUsername(), instruction.getEmailTemplateId());
        dynamicEmailDetails.addTemplateData("client", client.getClientName());
        dynamicEmailDetails.addTemplateData("subscriptionPlan", subscriptionPlanSnapshot.getPlanName());
        dynamicEmailDetails.addTemplateData("subscriptionPlanPeriod", subscriptionInvoice.getSubscriptionPeriod(client.getZoneId()));
        dynamicEmailDetails.addTemplateData("subscriptionAmount", helper.roundAmountAsString(() -> subscriptionInvoice.getDueAmount().getAmountWithoutTax()));
        dynamicEmailDetails.addTemplateData("subscriptionTax", helper.roundAmountAsString(() -> subscriptionInvoice.getDueAmount().getTax()));
        dynamicEmailDetails.addTemplateData("subscriptionAmountWithTax", helper.roundAmountAsString(() -> subscriptionInvoice.getDueAmount().getAmountWithTax()));

        notificationService.sendNotification(dynamicEmailDetails);

        return clientSubscriptionInvoiceRepository.save(subscriptionInvoice);
    }

    @Override
    public ClientSubscription getCurrentClientSubscription(String clientId) {
        return clientSubscriptionRepository.findFirstByClientIdOrderByCreatedDateDesc(clientId);
    }

    @Override
    public ClientSubscriptionInvoice getClientSubscriptionInvoiceByStatus(ClientSubscription clientSubscription) {
        return clientSubscriptionInvoiceRepository.findFirstByClientSubscriptionAndStatusOrderByCreatedDateDesc(clientSubscription, ClientSubscriptionInvoice.SubscriptionInvoiceStatus.PENDING);
    }

    @Override
    public ClientSubscriptionInvoice activateClientSubscription(ClientSubscriptionInvoice invoice) {

        invoice.setPaymentDate(new Date());
        invoice.setStatus(ClientSubscriptionInvoice.SubscriptionInvoiceStatus.PAID);

        final ClientSubscription clientSubscription = invoice.getClientSubscription();
        clientSubscription.setStatus(ClientSubscription.SubscriptionStatus.ACTIVE);

        if (clientSubscription.getPlanStartDate() == null) {
            clientSubscription.setPlanStartDate(new Date());
        }

        clientSubscriptionRepository.save(clientSubscription);

        return clientSubscriptionInvoiceRepository.save(invoice);
    }

    @Override
    public ClientSubscription lapseClientSubscription(ClientSubscription clientSubscription) {

        LOGGER.info("Lapsing the client subscription: {}", clientSubscription.getId());
        clientSubscription.setStatus(ClientSubscription.SubscriptionStatus.ACTIVE_LAPSING);

        final ClientSubscriptionInvoice subscriptionInvoice = clientSubscriptionInvoiceRepository.findFirstByClientSubscriptionAndStatusOrderByCreatedDateDesc(clientSubscription, ClientSubscriptionInvoice.SubscriptionInvoiceStatus.PAID);
        clientSubscription.setPlanEndDate(subscriptionInvoice.getValidTo());

        return this.saveClientSubscription(clientSubscription);
    }

    @Override
    public ClientSubscription saveClientSubscription(ClientSubscription clientSubscription) {
        return clientSubscriptionRepository.save(clientSubscription);
    }

    /**
     * followings are renewal scenarios.
     *
     * @return
     */

    @Override
    public List<ClientSubscriptionInvoice> findSubscriptionInvoicesForRenewal() {

        final Date tenDaysFromNow = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).plusDays(10).toInstant());
        final List<ClientSubscriptionInvoice> lapsingInvoices = clientSubscriptionInvoiceRepository.findAllByValidToBetweenAndStatus(
                new Date(),
                tenDaysFromNow,
                ClientSubscriptionInvoice.SubscriptionInvoiceStatus.PAID);

        return lapsingInvoices.stream()
                .filter(inv -> inv.getClientSubscription().getStatus() == ClientSubscription.SubscriptionStatus.ACTIVE)
                .map(inv -> {
                    final Client client = clientService.getClientOrThrows(inv.getClientSubscription().getClientId());
                    final ClientSubscriptionInvoice newSubscriptionInvoice = createAndSendClientSubscriptionInvoice(client, inv.getClientSubscription(), inv.getValidTo());

                    LOGGER.info("Created new subscription invoice: {}", newSubscriptionInvoice.getId());

                    return newSubscriptionInvoice;
                }).collect(Collectors.toList());
    }

    @Override
    public List<ClientSubscriptionInvoice> findUnpaidSubscriptionInvoices() {

        final List<ClientSubscriptionInvoice> unpaidInvoices = clientSubscriptionInvoiceRepository.findAllByDueDateBeforeAndStatus(new Date(), ClientSubscriptionInvoice.SubscriptionInvoiceStatus.PENDING);

        return unpaidInvoices.stream()
                .peek(inv -> {
                    LOGGER.info("Marking subscription invoice {} as OVERDUE and deactivate client subscription {}", inv.getId(), inv.getClientSubscription().getId());

                    inv.setStatus(ClientSubscriptionInvoice.SubscriptionInvoiceStatus.OVERDUE);
                    clientSubscriptionInvoiceRepository.save(inv);

                    deactivateClientSubscription(inv.getClientSubscription());
                }).collect(Collectors.toList());
    }

    private void deactivateClientSubscription(ClientSubscription clientSubscription) {

        clientSubscription.setStatus(ClientSubscription.SubscriptionStatus.INACTIVE);

        this.saveClientSubscription(clientSubscription);
    }


}
