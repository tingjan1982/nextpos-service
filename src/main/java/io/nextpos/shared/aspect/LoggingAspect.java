package io.nextpos.shared.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 
 */
@Component
@Aspect
public class LoggingAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingAspect.class);

    @Around(value = "services()")
    public Object process(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {

        try {
            final String method = proceedingJoinPoint.getSignature().toString();
            LOGGER.debug("Service method: {}, args: {}", method, Arrays.toString(proceedingJoinPoint.getArgs()));

            final Object result = proceedingJoinPoint.proceed();
            LOGGER.debug("Returns: {}", result);

            return result;
        } catch (Throwable e) {
            LOGGER.debug("Caught an exception: {}", e.getMessage(), e);

            throw e;
        }
    }

    @Pointcut("@within(org.springframework.stereotype.Service))")
    public void services() {

    }

}
