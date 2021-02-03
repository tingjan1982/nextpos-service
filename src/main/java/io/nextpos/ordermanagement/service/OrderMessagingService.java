package io.nextpos.ordermanagement.service;

public interface OrderMessagingService {

    void sendOrderLineItems(String clientId, boolean needAlert);
}
