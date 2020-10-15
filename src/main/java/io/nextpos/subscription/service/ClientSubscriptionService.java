package io.nextpos.subscription.service;

import io.nextpos.client.data.Client;
import io.nextpos.subscription.data.ClientSubscription;
import io.nextpos.subscription.data.ClientSubscriptionInvoice;
import io.nextpos.subscription.data.SubscriptionPlan;

import java.util.List;

public interface ClientSubscriptionService {

    ClientSubscription createClientSubscription(Client client, String subscriptionPlanId, SubscriptionPlan.PlanPeriod planPeriod);

    ClientSubscription getCurrentClientSubscription(String clientId);

    ClientSubscriptionInvoice getClientSubscriptionInvoiceByStatus(ClientSubscription clientSubscription);

    ClientSubscriptionInvoice activateClientSubscription(ClientSubscriptionInvoice invoice);

    ClientSubscription lapseClientSubscription(ClientSubscription clientSubscription);

    ClientSubscription saveClientSubscription(ClientSubscription clientSubscription);

    List<ClientSubscriptionInvoice> findSubscriptionInvoicesForRenewal();

    List<ClientSubscriptionInvoice> findUnpaidSubscriptionInvoices();
}
