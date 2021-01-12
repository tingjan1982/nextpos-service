package io.nextpos.ordermanagement.service;

import io.nextpos.ordermanagement.data.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

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
    public void sendOrders(final String clientId) {
        LOGGER.info("Sending updated in process orders for: {}", clientId);

        final List<Order> inProcessOrders = orderService.getOrdersByState(clientId, Order.OrderState.IN_PROCESS);
        messagingTemplate.convertAndSend("/dest/realtimeOrders/" + clientId, inProcessOrders);
    }
}
