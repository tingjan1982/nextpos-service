package io.nextpos.subscription.service;

import io.nextpos.subscription.data.ClientSubscriptionInvoice;

public interface ClientSubscriptionOrderService {

    void sendClientSubscriptionOrder(ClientSubscriptionInvoice clientSubscriptionInvoice);
}
