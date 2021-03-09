package io.nextpos.ordermanagement.web;

import io.nextpos.ordermanagement.data.InProcessOrderLineItems;
import io.nextpos.ordermanagement.data.InProcessOrders;
import io.nextpos.ordermanagement.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

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

    @MessageMapping("/realtimeOrderLineItems/{clientId}")
    @SendTo("/dest/realtimeOrderLineItems/{clientId}")
    public InProcessOrderLineItems realtimeOrderLineItems(@DestinationVariable String clientId) {

        return orderService.getInProcessOrderLineItems(clientId);
    }

    @MessageMapping("/realtimeOrders/{clientId}")
    @SendTo("/dest/realtimeOrders/{clientId}")
    public InProcessOrders realtimeOrders(@DestinationVariable String clientId) {

        return orderService.getInProcessOrders(clientId);
    }

    @MessageMapping("/inflightOrders/{clientId}")
    @SendTo("/dest/inflightOrders/{clientId}")
    public String inflightOrders(@DestinationVariable String clientId) {

        return clientId + ".inflightOrders.established";
    }

    @MessageMapping("/order/{orderId}")
    @SendTo("/dest/order/{orderId}")
    public String orderDetails(@DestinationVariable String orderId) {

        return orderId + ".order.established";
    }
}
