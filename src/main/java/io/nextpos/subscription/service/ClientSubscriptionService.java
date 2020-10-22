package io.nextpos.subscription.service;

import io.nextpos.client.data.Client;
import io.nextpos.subscription.data.ClientSubscription;
import io.nextpos.subscription.data.ClientSubscriptionInvoice;
import io.nextpos.subscription.data.SubscriptionPlan;

import java.util.List;

public interface ClientSubscriptionService {

    ClientSubscriptionInvoice createClientSubscription(Client client, String subscriptionPlanId, SubscriptionPlan.PlanPeriod planPeriod);

    ClientSubscription getCurrentClientSubscription(String clientId);

    ClientSubscription lapseClientSubscription(ClientSubscription clientSubscription);

    ClientSubscription saveClientSubscription(ClientSubscription clientSubscription);

    ClientSubscriptionInvoice activateClientSubscription(String invoiceIdentifier);

    ClientSubscriptionInvoice activateClientSubscription(ClientSubscriptionInvoice invoice);

    ClientSubscriptionInvoice getClientSubscriptionInvoice(String id);

    ClientSubscriptionInvoice getClientSubscriptionInvoiceByInvoiceIdentifier(String invoiceIdentifier);

    List<ClientSubscriptionInvoice> getClientSubscriptionInvoicesByStatus(ClientSubscriptionInvoice.SubscriptionInvoiceStatus pending);

    List<ClientSubscriptionInvoice> findSubscriptionInvoicesForRenewal();

    List<ClientSubscriptionInvoice> findUnpaidSubscriptionInvoices();

}
