package io.nextpos.shared.aspect;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderStateChange;
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

    @Autowired
    public WebSocketClientAspect(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Around(value = "onWebSocketClientOrders(webSocketClientOrders, order)", argNames = "proceedingJoinPoint,webSocketClientOrders,order")
    public Object notifyWebSocketClientOrders(ProceedingJoinPoint proceedingJoinPoint,
                                              WebSocketClientOrders webSocketClientOrders,
                                              Order order) throws Throwable {

        final Object result = proceedingJoinPoint.proceed();

        messagingTemplate.convertAndSend("/dest/inflightOrders/" + order.getClientId(), order.getClientId() + ".inflightOrders.ordersChanged");

        return result;
    }

    @AfterReturning(value = "onWebSocketClientOrder(webSocketClientOrder)", argNames = "webSocketClientOrder,result", returning = "result")
    public void notifyWebSocketClientOrder(WebSocketClientOrder webSocketClientOrder, Order result) {

        final String orderId = result.getId();

        messagingTemplate.convertAndSend("/dest/order/" + orderId, orderId + ".order.orderChanged");
    }

    @AfterReturning(value = "onWebSocketClientOrder(webSocketClientOrder)", argNames = "webSocketClientOrder,result", returning = "result")
    public void notifyWebSocketClientOrderReturningOSC(WebSocketClientOrder webSocketClientOrder, OrderStateChange result) {

        final String orderId = result.getOrderId();

        messagingTemplate.convertAndSend("/dest/order/" + orderId, orderId + ".order.orderChanged");
    }

    @Pointcut(value = "@annotation(webSocketClientOrders) && args(order, ..)", argNames = "webSocketClientOrders,order")
    public void onWebSocketClientOrders(WebSocketClientOrders webSocketClientOrders, Order order) {

    }

    @Pointcut(value = "@annotation(webSocketClientOrder) && args(..)", argNames = "webSocketClientOrder")
    public void onWebSocketClientOrder(WebSocketClientOrder webSocketClientOrder) {

    }
}
