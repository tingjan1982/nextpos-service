package io.nextpos.ordermanagement.service;

import io.nextpos.ordermanagement.data.InProcessOrderLineItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderMessagingServiceImpl implements OrderMessagingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderMessagingServiceImpl.class);

    private final OrderService orderService;

    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public OrderMessagingServiceImpl(final OrderService orderService, final SimpMessagingTemplate messagingTemplate) {
        this.orderService = orderService;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void sendOrderLineItems(final String clientId, boolean needAlert) {

        LOGGER.info("Sending updated in-process order line items for: {}", clientId);

        final InProcessOrderLineItems inProcessOrderLineItems = orderService.getInProcessOrderLineItems(clientId);
        inProcessOrderLineItems.setNeedAlert(needAlert);

        messagingTemplate.convertAndSend("/dest/realtimeOrders/" + clientId, inProcessOrderLineItems);
    }
}
