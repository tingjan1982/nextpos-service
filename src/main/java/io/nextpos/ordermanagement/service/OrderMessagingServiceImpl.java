package io.nextpos.ordermanagement.service;

import io.nextpos.ordermanagement.data.InProcessOrderLineItems;
import io.nextpos.ordermanagement.data.InProcessOrders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class OrderMessagingServiceImpl implements OrderMessagingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderMessagingServiceImpl.class);

    private final OrderService orderService;

    private final SimpMessagingTemplate messagingTemplate;

    private final RestTemplate restTemplate;

    @Value("${messageService.endpoint}")
    private String messageEndpoint;

    @Autowired
    public OrderMessagingServiceImpl(final OrderService orderService, final SimpMessagingTemplate messagingTemplate) {
        this.orderService = orderService;
        this.messagingTemplate = messagingTemplate;
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setConnectTimeout(1000);
        this.restTemplate = new RestTemplate(requestFactory);
    }

    @Override
    public void sendOrderLineItems(final String clientId, boolean needAlert) {

        LOGGER.info("Sending updated in-process order line items for: {}", clientId);

        final InProcessOrderLineItems inProcessOrderLineItems = orderService.getInProcessOrderLineItems(clientId);
        inProcessOrderLineItems.setNeedAlert(needAlert);

        messagingTemplate.convertAndSend("/topic/realtimeOrderLineItems/" + clientId, inProcessOrderLineItems);
    }

    @Override
    public void sendOrders(final String clientId, boolean needAlert) {

        LOGGER.info("Sending updated in-process order line items for: {}", clientId);

        final InProcessOrders inProcessOrders = orderService.getInProcessOrders(clientId);
        inProcessOrders.setNeedAlert(needAlert);

        messagingTemplate.convertAndSend("/topic/realtimeOrders/" + clientId, inProcessOrders);
    }

    @Override
    public void sendInFlightOrdersUpdate(String clientId) {

        try {
            restTemplate.exchange( messageEndpoint + "/messages/inflightOrders/{clientId}", HttpMethod.POST, null, String.class, clientId);

        } catch (Exception e) {
            logFailedUpdate(e.getMessage());
        }
    }

    private void logFailedUpdate(String message) {
        LOGGER.warn("Error while sending update message to message service: {}", message);
    }
}
