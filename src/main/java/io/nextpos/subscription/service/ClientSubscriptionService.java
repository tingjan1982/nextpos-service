package io.nextpos.subscription.service;

import io.nextpos.client.data.Client;
import io.nextpos.subscription.data.ClientSubscription;
import io.nextpos.subscription.data.ClientSubscriptionInvoice;
import io.nextpos.subscription.data.SubscriptionPlan;

import java.util.List;

public interface ClientSubscriptionService {

    ClientSubscriptionInvoice createClientSubscription(Client client, String subscriptionPlanId, SubscriptionPlan.PlanPeriod planPeriod);

    void sendClientSubscriptionInvoice(Client client, ClientSubscriptionInvoice subscriptionInvoice);

    ClientSubscription getCurrentClientSubscription(String clientId);

    ClientSubscription getClientSubscription(String id);

    ClientSubscription cancelClientSubscription(ClientSubscription clientSubscription);

    ClientSubscription lapseClientSubscription(ClientSubscription clientSubscription);

    ClientSubscription saveClientSubscription(ClientSubscription clientSubscription);

    List<ClientSubscription> getClientSubscriptions();

    ClientSubscriptionInvoice activateClientSubscriptionByInvoiceIdentifier(String invoiceIdentifier);

    ClientSubscription activateClientSubscription(ClientSubscription clientSubscription);

    ClientSubscription deactivateClientSubscription(ClientSubscription clientSubscription);

    ClientSubscriptionInvoice getClientSubscriptionInvoice(String id);

    ClientSubscriptionInvoice getClientSubscriptionInvoiceByInvoiceIdentifier(String invoiceIdentifier);

    List<ClientSubscriptionInvoice> getClientSubscriptionInvoices(ClientSubscription clientSubscription);

    List<ClientSubscriptionInvoice> getClientSubscriptionInvoicesByStatus(ClientSubscriptionInvoice.SubscriptionInvoiceStatus pending);

    List<ClientSubscriptionInvoice> findSubscriptionInvoicesForRenewal();

    List<ClientSubscriptionInvoice> findUnpaidSubscriptionInvoices();

}
