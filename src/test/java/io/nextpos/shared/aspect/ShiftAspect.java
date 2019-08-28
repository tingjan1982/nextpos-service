package io.nextpos.shared.aspect;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.service.ShiftService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * This aspect will automatically open shift when createOrder method is called.
 */
@Component
@Aspect
public class ShiftAspect {

    private final ShiftService shiftService;

    @Autowired
    public ShiftAspect(final ShiftService shiftService) {
        this.shiftService = shiftService;
    }

    @Around(value = "execution(* *.createOrder(..))")
    public Object process(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {

        final Order order = (Order) proceedingJoinPoint.getArgs()[0];
        shiftService.openShift(order.getClientId(), BigDecimal.ONE);

        return proceedingJoinPoint.proceed();
    }
}
