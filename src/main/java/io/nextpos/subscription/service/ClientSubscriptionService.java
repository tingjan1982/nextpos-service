package io.nextpos.subscription.service;

import io.nextpos.client.data.Client;
import io.nextpos.subscription.data.ClientSubscription;
import io.nextpos.subscription.data.ClientSubscriptionInvoice;
import io.nextpos.subscription.service.bean.CreateClientSubscription;

import java.util.Date;
import java.util.List;

public interface ClientSubscriptionService {

    ClientSubscriptionInvoice createClientSubscription(Client client, CreateClientSubscription createClientSubscription);

    ClientSubscriptionInvoice createClientSubscriptionInvoice(Client client, ClientSubscription clientSubscription, Date planStartDate);

    void sendClientSubscriptionInvoice(Client client, ClientSubscriptionInvoice subscriptionInvoice);

    ClientSubscriptionInvoice activateClientSubscriptionByInvoiceIdentifier(String invoiceIdentifier, boolean sendInvoiceNotification);

    ClientSubscription activateClientSubscription(ClientSubscription clientSubscription);

    ClientSubscription deactivateClientSubscription(ClientSubscription clientSubscription);

    ClientSubscription cancelClientSubscription(ClientSubscription clientSubscription);

    ClientSubscription lapseClientSubscription(ClientSubscription clientSubscription);

    ClientSubscription saveClientSubscription(ClientSubscription subscription);

    void deleteClientSubscriptionInvoice(String invoiceId);

    ClientSubscription getCurrentClientSubscription(String clientId);

    ClientSubscription getClientSubscription(String id);

    List<ClientSubscription> getClientSubscriptions();

    List<ClientSubscription> getClientSubscriptionsByStatus(ClientSubscription.SubscriptionStatus status);

    ClientSubscriptionInvoice getClientSubscriptionInvoice(String id);

    ClientSubscriptionInvoice getClientSubscriptionInvoiceByInvoiceIdentifier(String invoiceIdentifier);

    List<ClientSubscriptionInvoice> getClientSubscriptionInvoices(ClientSubscription clientSubscription);

    List<ClientSubscriptionInvoice> getClientSubscriptionInvoicesByStatuses(List<ClientSubscriptionInvoice.SubscriptionInvoiceStatus> statuses);
}
