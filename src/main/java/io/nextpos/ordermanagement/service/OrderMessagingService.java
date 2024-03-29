package io.nextpos.ordermanagement.service;

public interface OrderMessagingService {

    void sendOrderLineItems(String clientId, boolean needAlert);

    void sendOrders(String clientId, boolean needAlert);

    void sendInFlightOrdersUpdate(String clientId);

    void sendOrderUpdate(String orderId);
}
