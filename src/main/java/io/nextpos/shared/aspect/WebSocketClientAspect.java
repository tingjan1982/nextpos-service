package io.nextpos.shared.aspect;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderStateChange;
import io.nextpos.ordermanagement.service.OrderMessagingService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class WebSocketClientAspect {

    private final SimpMessagingTemplate messagingTemplate;

    private final OrderMessagingService orderMessagingService;

    @Autowired
    public WebSocketClientAspect(SimpMessagingTemplate messagingTemplate, OrderMessagingService orderMessagingService) {
        this.messagingTemplate = messagingTemplate;
        this.orderMessagingService = orderMessagingService;
    }

    @Around(value = "onWebSocketClientOrders(webSocketClientOrders, order)", argNames = "proceedingJoinPoint,webSocketClientOrders,order")
    public Object notifyWebSocketClientOrders(ProceedingJoinPoint proceedingJoinPoint,
                                              WebSocketClientOrders webSocketClientOrders,
                                              Order order) throws Throwable {

        final Object result = proceedingJoinPoint.proceed();

        messagingTemplate.convertAndSend("/topic/inflightOrders/" + order.getClientId(), order.getClientId() + ".inflightOrders.ordersChanged");
        //orderMessagingService.sendInFlightOrdersUpdate(order.getClientId());

        return result;
    }

    @AfterReturning(value = "onWebSocketClientOrder(webSocketClientOrder)", argNames = "webSocketClientOrder,result", returning = "result")
    public void notifyWebSocketClientOrder(WebSocketClientOrder webSocketClientOrder, Order result) {

        final String orderId = result.getId();

        sendOrderUpdate(orderId);
    }

    @AfterReturning(value = "onWebSocketClientOrder(webSocketClientOrder)", argNames = "webSocketClientOrder,result", returning = "result")
    public void notifyWebSocketClientOrderReturningOSC(WebSocketClientOrder webSocketClientOrder, OrderStateChange result) {

        final String orderId = result.getOrderId();

        sendOrderUpdate(orderId);
    }

    private void sendOrderUpdate(String orderId) {

        messagingTemplate.convertAndSend("/topic/order/" + orderId, orderId + ".order.orderChanged");
        //orderMessagingService.sendOrderUpdate(orderId);
    }

    @Pointcut(value = "@annotation(webSocketClientOrders) && args(order, ..)", argNames = "webSocketClientOrders,order")
    public void onWebSocketClientOrders(WebSocketClientOrders webSocketClientOrders, Order order) {

    }

    @Pointcut(value = "@annotation(webSocketClientOrder) && args(..)", argNames = "webSocketClientOrder")
    public void onWebSocketClientOrder(WebSocketClientOrder webSocketClientOrder) {

    }
}
