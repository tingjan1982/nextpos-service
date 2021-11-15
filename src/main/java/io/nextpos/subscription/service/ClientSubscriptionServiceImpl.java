package io.nextpos.subscription.service;

import io.nextpos.client.data.Client;
import io.nextpos.notification.data.DynamicEmailDetails;
import io.nextpos.notification.service.NotificationService;
import io.nextpos.settings.data.CountrySettings;
import io.nextpos.settings.service.SettingsService;
import io.nextpos.shared.exception.BusinessLogicException;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import io.nextpos.subscription.data.*;
import io.nextpos.subscription.service.bean.CreateClientSubscription;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@ChainedTransaction
public class ClientSubscriptionServiceImpl implements ClientSubscriptionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSubscriptionServiceImpl.class);

    private final SubscriptionPlanService subscriptionPlanService;

    private final ClientSubscriptionOrderService clientSubscriptionOrderService;

    private final NotificationService notificationService;

    private final SettingsService settingsService;

    private final ClientSubscriptionRepository clientSubscriptionRepository;

    private final ClientSubscriptionInvoiceRepository clientSubscriptionInvoiceRepository;

    @Autowired
    public ClientSubscriptionServiceImpl(SubscriptionPlanService subscriptionPlanService, ClientSubscriptionOrderService clientSubscriptionOrderService, NotificationService notificationService, SettingsService settingsService, ClientSubscriptionRepository clientSubscriptionRepository, ClientSubscriptionInvoiceRepository clientSubscriptionInvoiceRepository) {
        this.subscriptionPlanService = subscriptionPlanService;
        this.clientSubscriptionOrderService = clientSubscriptionOrderService;
        this.notificationService = notificationService;
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

        return createAndSendClientSubscriptionInvoice(client, clientSubscription, validFrom, false, true);
    }

    @Override
    public ClientSubscriptionInvoice createClientSubscriptionInvoice(Client client, ClientSubscription clientSubscription, Date planStartDate) {
        return this.createAndSendClientSubscriptionInvoice(client, clientSubscription, planStartDate, true, true);
    }

    @Override
    public ClientSubscriptionInvoice createAndSendClientSubscriptionInvoice(Client client, ClientSubscription clientSubscription, Date subscriptionValidFrom, boolean renewal, boolean sendInvoice) {

        final ClientSubscriptionInvoice subscriptionInvoice = new ClientSubscriptionInvoice(client.getZoneId(), clientSubscription, subscriptionValidFrom);
        final ClientSubscriptionInvoice saved = clientSubscriptionInvoiceRepository.save(subscriptionInvoice);

        clientSubscription.setCurrentInvoiceId(saved.getId());

        if (renewal) {
            clientSubscription.setStatus(ClientSubscription.SubscriptionStatus.ACTIVE_RENEWING);
        }

        saveClientSubscription(clientSubscription);

        if (sendInvoice) {
            this.sendClientSubscriptionInvoice(client, subscriptionInvoice);
        }

        return saved;
    }

    @Override
    public void sendClientSubscriptionInvoice(Client client, ClientSubscriptionInvoice subscriptionInvoice) {
        this.sendClientSubscriptionInvoice(client, subscriptionInvoice, null);
    }

    @Override
    public void sendClientSubscriptionInvoice(Client client, ClientSubscriptionInvoice subscriptionInvoice, String overrideEmail) {

        final SubscriptionPlan subscriptionPlanSnapshot = subscriptionInvoice.getClientSubscription().getSubscriptionPlanSnapshot();
        final SubscriptionPaymentInstruction instruction = subscriptionPlanService.getSubscriptionPaymentInstructionByCountryOrThrows(subscriptionPlanSnapshot.getCountryCode());

        final CountrySettings countrySettings = settingsService.getCountrySettings(client.getCountryCode());
        final CountrySettings.RoundingAmountHelper helper = countrySettings.roundingAmountHelper();

        String emailToUse = client.getUsername();

        if (StringUtils.isNotBlank(overrideEmail)) {
            emailToUse = overrideEmail;
        }

        final DynamicEmailDetails dynamicEmailDetails = new DynamicEmailDetails(client.getId(), emailToUse, instruction.getEmailTemplateId());
        dynamicEmailDetails.addTemplateData("client", client.getClientName());
        dynamicEmailDetails.addTemplateData("subscriptionPlan", subscriptionPlanSnapshot.getPlanName());
        dynamicEmailDetails.addTemplateData("subscriptionPlanPeriod", subscriptionInvoice.getSubscriptionPeriod(client.getZoneId()));
        dynamicEmailDetails.addTemplateData("subscriptionAmount", helper.roundAmountAsString(() -> subscriptionInvoice.getDueAmount().getAmountWithoutTax()));
        dynamicEmailDetails.addTemplateData("subscriptionTax", helper.roundAmountAsString(() -> subscriptionInvoice.getDueAmount().getTax()));
        dynamicEmailDetails.addTemplateData("subscriptionAmountWithTax", helper.roundAmountAsString(() -> subscriptionInvoice.getDueAmount().getAmountWithTax()));
        dynamicEmailDetails.addTemplateData("invoiceIdentifier", subscriptionInvoice.getInvoiceIdentifier());

        notificationService.sendSimpleNotification(dynamicEmailDetails);
    }

    @Override
    public void deleteClientSubscriptionInvoice(String invoiceId) {

        final ClientSubscriptionInvoice invoiceToDelete = getClientSubscriptionInvoice(invoiceId);

        if (invoiceToDelete.getStatus() != ClientSubscriptionInvoice.SubscriptionInvoiceStatus.PENDING) {
            throw new BusinessLogicException("Non pending invoice cannot be deleted");
        }

        final ClientSubscription clientSubscription = invoiceToDelete.getClientSubscription();
        final List<ClientSubscriptionInvoice> invoices = getClientSubscriptionInvoices(clientSubscription);

        if (invoices.size() == 1) {
            throw new BusinessLogicException("This is the first ever invoice and cannot be deleted");
        }

        clientSubscriptionInvoiceRepository.delete(invoiceToDelete);

        invoices.stream()
                .filter(inv -> !StringUtils.equals(inv.getId(), invoiceToDelete.getId()))
                .findFirst()
                .ifPresent(lastInv -> {
                    clientSubscription.setCurrentInvoiceId(lastInv.getId());
                    this.saveClientSubscription(clientSubscription);
                });
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
    public List<ClientSubscription> getClientSubscriptions() {
        return clientSubscriptionRepository.findAllByCurrentIsTrueOrStatus(ClientSubscription.SubscriptionStatus.SUBMITTED);
    }

    @Override
    public List<ClientSubscription> getClientSubscriptionsByStatus(ClientSubscription.SubscriptionStatus status) {
        return clientSubscriptionRepository.findAllByStatus(status);
    }

    /**
     * Main subscription activation method.
     */
    @Override
    public ClientSubscriptionInvoice activateClientSubscriptionByInvoiceIdentifier(String invoiceIdentifier, boolean sendInvoiceNotification) {

        final ClientSubscriptionInvoice clientSubscriptionInvoice = clientSubscriptionInvoiceRepository.findByInvoiceIdentifier(invoiceIdentifier);

        if (clientSubscriptionInvoice.getStatus() == ClientSubscriptionInvoice.SubscriptionInvoiceStatus.PAID) {
            return clientSubscriptionInvoice;
        }

        clientSubscriptionInvoice.updatePaymentStatus(new Date());

        if (!clientSubscriptionInvoice.isInvoiceSent() && sendInvoiceNotification) {
            clientSubscriptionOrderService.sendClientSubscriptionOrder(clientSubscriptionInvoice, null);
            clientSubscriptionInvoice.setInvoiceSent(true);
        }

        activateClientSubscription(clientSubscriptionInvoice.getClientSubscription());

        return clientSubscriptionInvoiceRepository.save(clientSubscriptionInvoice);
    }

    /**
     * Activate ClientSubscription only, does not affect ClientSubscriptionInvoice.
     */
    @Override
    public ClientSubscription activateClientSubscription(ClientSubscription clientSubscription) {

        clientSubscription.setStatus(ClientSubscription.SubscriptionStatus.ACTIVE);

        ClientSubscriptionInvoice invoice = this.getClientSubscriptionInvoice(clientSubscription.getCurrentInvoiceId());
        clientSubscription.setPlanStartDate(invoice.getValidFrom());
        clientSubscription.setPlanEndDate(invoice.getValidTo());

        // this handles two active subscription scenarios where new one takes over previous one.
        if (!clientSubscription.isCurrent()) {
            final ClientSubscription outgoingClientSubscription = getCurrentClientSubscription(clientSubscription.getClientId());
            outgoingClientSubscription.setCurrent(false);
            outgoingClientSubscription.setPlanEndDate(new Date());
            outgoingClientSubscription.setStatus(ClientSubscription.SubscriptionStatus.LAPSED);
            saveClientSubscription(outgoingClientSubscription);

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
    public ClientSubscription saveClientSubscription(ClientSubscription clientSubscription) {
        return clientSubscriptionRepository.save(clientSubscription);
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
        return clientSubscriptionInvoiceRepository.findAllByClientSubscriptionOrderByValidToDesc(clientSubscription);
    }

    @Override
    public List<ClientSubscriptionInvoice> getClientSubscriptionInvoicesByStatuses(List<ClientSubscriptionInvoice.SubscriptionInvoiceStatus> status) {
        return clientSubscriptionInvoiceRepository.findAllByStatusIn(status);
    }
}
