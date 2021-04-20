package io.nextpos.subscription.scheduler;

import io.nextpos.subscription.data.ClientSubscription;
import io.nextpos.subscription.data.ClientSubscriptionInvoice;
import io.nextpos.subscription.service.ClientSubscriptionLifecycleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ClientSubscriptionInvoiceScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSubscriptionInvoiceScheduler.class);

    private final ClientSubscriptionLifecycleService clientSubscriptionLifecycleService;

    @Autowired
    public ClientSubscriptionInvoiceScheduler(ClientSubscriptionLifecycleService clientSubscriptionLifecycleService) {
        this.clientSubscriptionLifecycleService = clientSubscriptionLifecycleService;
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void findSubscriptionInvoicesForRenewal() {

        LOGGER.info("[Renewal subscription invoices]: Start");

        final List<ClientSubscriptionInvoice> invoices = clientSubscriptionLifecycleService.findSubscriptionInvoicesForRenewal();

        LOGGER.info("[Renewal subscription invoices]: Start ({})", invoices.size());
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void findUnpaidSubscriptionInvoices() {

        LOGGER.info("[Unpaid subscription invoices]: Start");

        final List<ClientSubscriptionInvoice> invoices = clientSubscriptionLifecycleService.findUnpaidSubscriptionInvoices();

        LOGGER.info("[Unpaid subscription invoices]: Complete ({})", invoices.size());
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void processLapsingClientSubscriptions() {

        LOGGER.info("[Lapsing client subscriptions]: Start");

        final List<ClientSubscription> clientSubscriptions = clientSubscriptionLifecycleService.processActiveLapsingClientSubscriptions();

        LOGGER.info("[Lapsing client subscriptions]: Complete ({})", clientSubscriptions.size());
    }
}
