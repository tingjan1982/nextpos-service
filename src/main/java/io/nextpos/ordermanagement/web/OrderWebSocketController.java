package io.nextpos.ordermanagement.web;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * Reference on WebSocket
 *
 * https://spring.io/guides/gs/messaging-stomp-websocket/
 * https://docs.spring.io/autorepo/docs/spring-security/4.0.x/reference/html/websocket.html
 * https://www.toptal.com/java/stomp-spring-boot-websocket
 */
@Controller
public class OrderWebSocketController {

    private final OrderService orderService;

    @Autowired
    public OrderWebSocketController(final OrderService orderService) {
        this.orderService = orderService;
    }

    @MessageMapping("/realtimeOrders")
    @SendTo("/dest/realtimeOrders")
    public List<Order> realtimeOrders(String clientId) {
        return orderService.getOrdersByState(clientId, Order.OrderState.IN_PROCESS);
    }
}
