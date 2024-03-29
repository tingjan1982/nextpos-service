package io.nextpos.subscription.service;

import io.nextpos.subscription.data.ClientSubscription;
import io.nextpos.subscription.data.ClientSubscriptionInvoice;

import java.util.List;

public interface ClientSubscriptionLifecycleService {

    List<ClientSubscription> findClientSubscriptionsUpForRenewal();

    List<ClientSubscriptionInvoice> renewActiveClientSubscriptions();

    List<ClientSubscriptionInvoice> findUnpaidSubscriptionInvoices();

    List<ClientSubscription> processActiveLapsingClientSubscriptions();
}
