package io.nextpos.subscription.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.notification.data.DynamicEmailDetails;
import io.nextpos.notification.service.NotificationService;
import io.nextpos.settings.data.CountrySettings;
import io.nextpos.settings.service.SettingsService;
import io.nextpos.shared.exception.BusinessLogicException;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import io.nextpos.subscription.data.*;
import io.nextpos.subscription.service.bean.CreateClientSubscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@ChainedTransaction
public class ClientSubscriptionServiceImpl implements ClientSubscriptionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSubscriptionServiceImpl.class);

    private final SubscriptionPlanService subscriptionPlanService;

    private final ClientSubscriptionOrderService clientSubscriptionOrderService;

    private final NotificationService notificationService;

    private final ClientService clientService;

    private final SettingsService settingsService;

    private final ClientSubscriptionRepository clientSubscriptionRepository;

    private final ClientSubscriptionInvoiceRepository clientSubscriptionInvoiceRepository;

    @Autowired
    public ClientSubscriptionServiceImpl(SubscriptionPlanService subscriptionPlanService, ClientSubscriptionOrderService clientSubscriptionOrderService, NotificationService notificationService, ClientService clientService, SettingsService settingsService, ClientSubscriptionRepository clientSubscriptionRepository, ClientSubscriptionInvoiceRepository clientSubscriptionInvoiceRepository) {
        this.subscriptionPlanService = subscriptionPlanService;
        this.clientSubscriptionOrderService = clientSubscriptionOrderService;
        this.notificationService = notificationService;
        this.clientService = clientService;
        this.settingsService = settingsService;
        this.clientSubscriptionRepository = clientSubscriptionRepository;
        this.clientSubscriptionInvoiceRepository = clientSubscriptionInvoiceRepository;
    }

    @Override
    public ClientSubscriptionInvoice createClientSubscription(Client client, CreateClientSubscription createClientSubscription) {

        final String subscriptionPlanId = createClientSubscription.getSubscriptionPlanId();
        final SubscriptionPlan.PlanPeriod planPeriod = createClientSubscription.getPlanPeriod();
        final Optional<ClientSubscription> clientSubscriptionOptional = clientSubscriptionRepository.findFirstByClientIdAndSubscriptionPlanSnapshot_IdAndPlanPeriodOrderByCreatedDateDesc(client.getId(), subscriptionPlanId, planPeriod);

        if (clientSubscriptionOptional.isPresent()) {
            final ClientSubscription clientSubscription = clientSubscriptionOptional.get();

            if (clientSubscription.isActiveSubscription()) {
                return getClientSubscriptionInvoice(clientSubscription.getCurrentInvoiceId());
            }
        }

        return manageCurrentAndNewSubscription(client, createClientSubscription);
    }

    private ClientSubscriptionInvoice manageCurrentAndNewSubscription(Client client, CreateClientSubscription createClientSubscription) {

        final ClientSubscription currentClientSubscription = getCurrentClientSubscription(client.getId());
        Date validFrom = new Date();
        boolean current = true;

        if (currentClientSubscription != null) {
            switch (currentClientSubscription.getStatus()) {
                case SUBMITTED:
                    clientSubscriptionRepository.delete(currentClientSubscription);
                    break;
                    
                case LAPSED:
                case INACTIVE:
                case CANCELLED:
                    currentClientSubscription.setCurrent(false);
                    saveClientSubscription(currentClientSubscription);
                    break;

                case ACTIVE_RENEWING:
                    final ClientSubscriptionInvoice renewalInvoice = getClientSubscriptionInvoice(currentClientSubscription.getCurrentInvoiceId());
                    renewalInvoice.setStatus(ClientSubscriptionInvoice.SubscriptionInvoiceStatus.CANCELLED);
                    clientSubscriptionInvoiceRepository.save(renewalInvoice);

                case ACTIVE:
                case ACTIVE_LAPSING:
                    final ClientSubscriptionInvoice clientSubscriptionInvoice = getClientSubscriptionInvoice(currentClientSubscription.getCurrentInvoiceId());
                    validFrom = clientSubscriptionInvoice.getValidTo();
                    current = false;
                    break;
            }
        }

        SubscriptionPlan subscription = subscriptionPlanService.getSubscription(createClientSubscription.getSubscriptionPlanId());
        final ClientSubscription clientSubscription = new ClientSubscription(client.getId(),
                subscription,
                createClientSubscription.getPlanPeriod(),
                createClientSubscription.getDiscountAmount());

        clientSubscription.setCurrent(current);
        this.saveClientSubscription(clientSubscription);

        return createAndSendClientSubscriptionInvoice(client, clientSubscription, validFrom, false);
    }

    private ClientSubscriptionInvoice createAndSendClientSubscriptionInvoice(Client client, ClientSubscription clientSubscription, Date subscriptionValidFrom, boolean renewal) {

        final ClientSubscriptionInvoice subscriptionInvoice = new ClientSubscriptionInvoice(client.getZoneId(), clientSubscription, subscriptionValidFrom, renewal);
        final ClientSubscriptionInvoice saved = clientSubscriptionInvoiceRepository.save(subscriptionInvoice);

        clientSubscription.setCurrentInvoiceId(saved.getId());

        if (renewal) {
            clientSubscription.setStatus(ClientSubscription.SubscriptionStatus.ACTIVE_RENEWING);
        }

        saveClientSubscription(clientSubscription);

        this.sendClientSubscriptionInvoice(client, subscriptionInvoice);

        return saved;
    }

    @Override
    public void sendClientSubscriptionInvoice(Client client, ClientSubscriptionInvoice subscriptionInvoice) {

        final SubscriptionPlan subscriptionPlanSnapshot = subscriptionInvoice.getClientSubscription().getSubscriptionPlanSnapshot();
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
        dynamicEmailDetails.addTemplateData("invoiceIdentifier", subscriptionInvoice.getInvoiceIdentifier());

        notificationService.sendNotification(dynamicEmailDetails);
    }

    @Override
    public ClientSubscription getCurrentClientSubscription(String clientId) {
        return clientSubscriptionRepository.findByClientIdAndCurrentIsTrue(clientId);
    }

    @Override
    public ClientSubscription getClientSubscription(String id) {
        return clientSubscriptionRepository.findById(id).orElseThrow(() -> {
            throw new ObjectNotFoundException(id, ClientSubscription.class);
        });
    }

    @Override
    public ClientSubscription cancelClientSubscription(ClientSubscription clientSubscription) {

        clientSubscription.setPlanEndDate(new Date());
        clientSubscription.setStatus(ClientSubscription.SubscriptionStatus.CANCELLED);

        ClientSubscriptionInvoice clientSubscriptionInvoice = getClientSubscriptionInvoice(clientSubscription.getCurrentInvoiceId());

        if (clientSubscriptionInvoice.getStatus() != ClientSubscriptionInvoice.SubscriptionInvoiceStatus.PAID) {
            clientSubscriptionInvoice.setStatus(ClientSubscriptionInvoice.SubscriptionInvoiceStatus.CANCELLED);
            clientSubscriptionInvoiceRepository.save(clientSubscriptionInvoice);
        }

        return saveClientSubscription(clientSubscription);
    }

    @Override
    public ClientSubscription lapseClientSubscription(ClientSubscription clientSubscription) {

        LOGGER.info("Lapsing the client subscription: {}", clientSubscription.getId());

        if (!clientSubscription.isActiveSubscription()) {
            throw new BusinessLogicException("message.notActive", "Client subscription is not active to perform lapse action");
        }

        clientSubscription.setStatus(ClientSubscription.SubscriptionStatus.ACTIVE_LAPSING);

        ClientSubscriptionInvoice clientSubscriptionInvoice = getClientSubscriptionInvoice(clientSubscription.getCurrentInvoiceId());

        if (clientSubscriptionInvoice.getStatus() != ClientSubscriptionInvoice.SubscriptionInvoiceStatus.PAID) {
            clientSubscriptionInvoice.setStatus(ClientSubscriptionInvoice.SubscriptionInvoiceStatus.CANCELLED);
            clientSubscriptionInvoiceRepository.save(clientSubscriptionInvoice);
        }

        return this.saveClientSubscription(clientSubscription);
    }

    @Override
    public ClientSubscription saveClientSubscription(ClientSubscription clientSubscription) {
        return clientSubscriptionRepository.save(clientSubscription);
    }

    @Override
    public List<ClientSubscription> getClientSubscriptions() {
        return clientSubscriptionRepository.findAllByCurrentIsTrueOrStatus(ClientSubscription.SubscriptionStatus.SUBMITTED);
    }

    @Override
    public ClientSubscriptionInvoice activateClientSubscriptionByInvoiceIdentifier(String invoiceIdentifier) {

        final ClientSubscriptionInvoice clientSubscriptionInvoice = clientSubscriptionInvoiceRepository.findByInvoiceIdentifier(invoiceIdentifier);

        if (clientSubscriptionInvoice.getStatus() == ClientSubscriptionInvoice.SubscriptionInvoiceStatus.PAID) {
            return clientSubscriptionInvoice;
        }

        clientSubscriptionInvoice.updatePaymentStatus(new Date());

        if (!clientSubscriptionInvoice.isInvoiceSent()) {
            clientSubscriptionOrderService.sendClientSubscriptionOrder(clientSubscriptionInvoice);
            clientSubscriptionInvoice.setInvoiceSent(true);
        }

        activateClientSubscription(clientSubscriptionInvoice.getClientSubscription());

        return clientSubscriptionInvoiceRepository.save(clientSubscriptionInvoice);
    }

    @Override
    public ClientSubscription activateClientSubscription(ClientSubscription clientSubscription) {

        clientSubscription.setStatus(ClientSubscription.SubscriptionStatus.ACTIVE);
        final Date now = new Date();

        if (clientSubscription.getPlanStartDate() == null) {
            clientSubscription.setPlanStartDate(now);
        }

        if (clientSubscription.getPlanEndDate() == null) {
            ClientSubscriptionInvoice invoice = this.getClientSubscriptionInvoice(clientSubscription.getCurrentInvoiceId());
            clientSubscription.setPlanEndDate(invoice.getValidTo());
        }

        // this handles two active subscription scenarios where new one takes over previous one.
        if (!clientSubscription.isCurrent()) {
            final ClientSubscription currentClientSubscription = getCurrentClientSubscription(clientSubscription.getClientId());
            currentClientSubscription.setCurrent(false);
            currentClientSubscription.setPlanEndDate(now);
            currentClientSubscription.setStatus(ClientSubscription.SubscriptionStatus.LAPSED);
            saveClientSubscription(currentClientSubscription);

            clientSubscription.setCurrent(true);
        }

        return saveClientSubscription(clientSubscription);
    }

    @Override
    public ClientSubscription deactivateClientSubscription(ClientSubscription clientSubscription) {

        clientSubscription.setStatus(ClientSubscription.SubscriptionStatus.INACTIVE);

        return saveClientSubscription(clientSubscription);
    }

    @Override
    public ClientSubscriptionInvoice getClientSubscriptionInvoice(String id) {
        return clientSubscriptionInvoiceRepository.findById(id).orElseThrow(() -> {
            throw new ObjectNotFoundException(id, ClientSubscriptionInvoice.class);
        });
    }

    @Override
    public ClientSubscriptionInvoice getClientSubscriptionInvoiceByInvoiceIdentifier(String invoiceIdentifier) {
        return clientSubscriptionInvoiceRepository.findByInvoiceIdentifier(invoiceIdentifier);
    }

    @Override
    public List<ClientSubscriptionInvoice> getClientSubscriptionInvoices(ClientSubscription clientSubscription) {
        return clientSubscriptionInvoiceRepository.findAllByClientSubscription(clientSubscription);
    }

    @Override
    public List<ClientSubscriptionInvoice> getClientSubscriptionInvoicesByStatuses(List<ClientSubscriptionInvoice.SubscriptionInvoiceStatus> status) {
        return clientSubscriptionInvoiceRepository.findAllByStatusIn(status);
    }

    /**
     * following methods are renewal scenarios.
     */
    @Override
    public List<ClientSubscriptionInvoice> findSubscriptionInvoicesForRenewal() {

        final Date tenDaysFromNow = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).plusDays(10).toInstant());
        final List<ClientSubscription> activeSubscriptions = clientSubscriptionRepository.findAllByStatusAndPlanEndDateBetween(
                ClientSubscription.SubscriptionStatus.ACTIVE,
                new Date(),
                tenDaysFromNow);

        return activeSubscriptions.stream()
                .map(sub -> {
                    final Client client = clientService.getClientOrThrows(sub.getClientId());
                    final ClientSubscriptionInvoice newSubscriptionInvoice = createAndSendClientSubscriptionInvoice(client, sub, sub.getPlanEndDate(), true);

                    LOGGER.info("Created new subscription invoice: {}", newSubscriptionInvoice.getId());

                    return newSubscriptionInvoice;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ClientSubscriptionInvoice> findUnpaidSubscriptionInvoices() {

        final List<ClientSubscriptionInvoice> unpaidInvoices = clientSubscriptionInvoiceRepository.findAllByDueDateBeforeAndStatus(new Date(), ClientSubscriptionInvoice.SubscriptionInvoiceStatus.PENDING);

        return unpaidInvoices.stream()
                .peek(inv -> {
                    LOGGER.info("Marking subscription invoice {} as OVERDUE", inv.getId());

                    inv.setStatus(ClientSubscriptionInvoice.SubscriptionInvoiceStatus.OVERDUE);
                    clientSubscriptionInvoiceRepository.save(inv);

                }).collect(Collectors.toList());
    }
}
