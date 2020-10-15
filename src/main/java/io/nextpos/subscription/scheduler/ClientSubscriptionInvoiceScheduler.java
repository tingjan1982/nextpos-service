package io.nextpos.subscription.scheduler;

import io.nextpos.subscription.data.ClientSubscriptionInvoice;
import io.nextpos.subscription.service.ClientSubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ClientSubscriptionInvoiceScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSubscriptionInvoiceScheduler.class);
    private final ClientSubscriptionService clientSubscriptionService;

    @Autowired
    public ClientSubscriptionInvoiceScheduler(ClientSubscriptionService clientSubscriptionService) {
        this.clientSubscriptionService = clientSubscriptionService;
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void findSubscriptionInvoicesForRenewal() {

        LOGGER.info("Start finding subscription invoices for renewal");

        final List<ClientSubscriptionInvoice> invoices = clientSubscriptionService.findSubscriptionInvoicesForRenewal();

        LOGGER.info("Found {} invoices and sent out renewal notifications to client", invoices.size());
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void findUnpaidSubscriptionInvoices() {

        LOGGER.info("Start finding unpaid subscription invoices");

        final List<ClientSubscriptionInvoice> invoices = clientSubscriptionService.findUnpaidSubscriptionInvoices();

        LOGGER.info("Found {} unpaid invoices and have deactivated the client account", invoices.size());
    }
}
