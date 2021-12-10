package io.nextpos.shared.aspect;

import io.nextpos.client.data.Client;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.shared.exception.BusinessLogicException;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Aspect
@Order(0)
public class OrderStateAspect {

    private final OrderService orderService;

    @Autowired
    public OrderStateAspect(OrderService orderService) {
        this.orderService = orderService;
    }


    @Before(value = "validateOrderStateMethods(validateOrderState, client, id)", argNames = "validateOrderState,client,id")
    public void validateOrderInPayment(ValidateOrderState validateOrderState,
                                       Client client,
                                       String id) throws Throwable {

        io.nextpos.ordermanagement.data.Order order = orderService.getOrder(id);

        if (order.isPaying() || order.isClosed()) {
            throw new BusinessLogicException("message.orderClosed", "Order is either finalized or in a sealed state to prevent further line item changes: " + order.getId());
        }
    }

    @Pointcut(value = "@annotation(validateOrderState) && args(client, id, ..)", argNames = "validateOrderState,client,id")
    public void validateOrderStateMethods(ValidateOrderState validateOrderState, Client client, String id) {

    }
}
