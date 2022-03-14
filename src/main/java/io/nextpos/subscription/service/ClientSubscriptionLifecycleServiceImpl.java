package io.nextpos.subscription.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import io.nextpos.shared.util.DateTimeUtil;
import io.nextpos.subscription.data.ClientSubscription;
import io.nextpos.subscription.data.ClientSubscriptionInvoice;
import io.nextpos.subscription.data.ClientSubscriptionInvoiceRepository;
import io.nextpos.subscription.data.ClientSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@ChainedTransaction
public class ClientSubscriptionLifecycleServiceImpl implements ClientSubscriptionLifecycleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSubscriptionLifecycleServiceImpl.class);

    private final ClientSubscriptionService clientSubscriptionService;

    private final ClientService clientService;

    private final ClientSubscriptionRepository clientSubscriptionRepository;

    private final ClientSubscriptionInvoiceRepository clientSubscriptionInvoiceRepository;

    @Autowired
    public ClientSubscriptionLifecycleServiceImpl(ClientSubscriptionService clientSubscriptionService, ClientService clientService, ClientSubscriptionRepository clientSubscriptionRepository, ClientSubscriptionInvoiceRepository clientSubscriptionInvoiceRepository) {
        this.clientSubscriptionService = clientSubscriptionService;
        this.clientService = clientService;
        this.clientSubscriptionRepository = clientSubscriptionRepository;
        this.clientSubscriptionInvoiceRepository = clientSubscriptionInvoiceRepository;
    }

    @Override
    public List<ClientSubscription> findClientSubscriptionsUpForRenewal() {

        LocalDateTime endDt = LocalDate.now().atStartOfDay().with(TemporalAdjusters.firstDayOfNextMonth());
        ZoneId zone = ZoneId.systemDefault();

        return this.findActiveClientSubscriptionByDate(new Date(), DateTimeUtil.toDate(zone, endDt));
    }

    @Override
    public List<ClientSubscriptionInvoice> renewActiveClientSubscriptions() {

        final List<ClientSubscription> activeSubscriptions = this.findActiveClientSubscriptionByDate(new Date(), computeDaysFromDate(LocalDate.now()));

        return activeSubscriptions.stream()
                .map(sub -> {
                    final Client client = clientService.getClientOrThrows(sub.getClientId());
                    return clientSubscriptionService.createClientSubscriptionInvoice(client, sub, sub.getPlanEndDate());
                })
                .collect(Collectors.toList());
    }

    Date computeDaysFromDate(LocalDate localDate) {

        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime dateInFuture = localDate.atTime(23, 59, 59).plusDays(10);

        if (dateInFuture.getMonth() != localDate.getMonth()) {
            dateInFuture = dateInFuture.withMonth(localDate.getMonthValue()).with(TemporalAdjusters.lastDayOfMonth());
        }

        return DateTimeUtil.toDate(zone, dateInFuture);
    }

    private List<ClientSubscription> findActiveClientSubscriptionByDate(Date from, Date to) {

        LOGGER.info("Finding client subscription with date range: {} - {}", from, to);

        List<ClientSubscription> clientSubscriptions = clientSubscriptionRepository.findAllByStatusAndPlanEndDateBetween(
                ClientSubscription.SubscriptionStatus.ACTIVE,
                from,
                to);

        return clientSubscriptions.stream()
                .filter(sub -> clientService.getClient(sub.getClientId()).isPresent())
                .collect(Collectors.toList());
    }


    @Override
    public List<ClientSubscriptionInvoice> findUnpaidSubscriptionInvoices() {

        final List<ClientSubscriptionInvoice> unpaidInvoices = clientSubscriptionInvoiceRepository.findAllByDueDateBeforeAndStatus(new Date(), ClientSubscriptionInvoice.SubscriptionInvoiceStatus.PENDING);

        return unpaidInvoices.stream()
                .peek(inv -> {
//                    inv.setStatus(ClientSubscriptionInvoice.SubscriptionInvoiceStatus.OVERDUE);
//                    clientSubscriptionInvoiceRepository.save(inv);

                }).collect(Collectors.toList());
    }

    @Override
    public List<ClientSubscription> processActiveLapsingClientSubscriptions() {

        final List<ClientSubscription> subscriptions = clientSubscriptionService.getClientSubscriptionsByStatus(ClientSubscription.SubscriptionStatus.ACTIVE_LAPSING);
        LOGGER.info("Active lapsing clients: {}", subscriptions.size());
        final Date now = new Date();

        return subscriptions.stream()
                .peek(sub -> {
                    if (sub.getPlanEndDate().before(now)) {
                        sub.setStatus(ClientSubscription.SubscriptionStatus.LAPSED);
                        clientSubscriptionRepository.save(sub);
                    }
                }).collect(Collectors.toList());
    }
}
