package io.nextpos.shared.aspect;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLog;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.shared.auth.OAuth2Helper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionUsageException;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@Aspect
public class OrderLogAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderLogAspect.class);

    private final OrderService orderService;

    private final OAuth2Helper oAuth2Helper;

    private final MongoTransactionManager mongoTransactionManager;

    private final RetryTemplate retryTemplate;

    @Autowired
    public OrderLogAspect(final OrderService orderService, final OAuth2Helper oAuth2Helper, final MongoTransactionManager mongoTransactionManager, final RetryTemplate retryTemplate) {
        this.orderService = orderService;
        this.oAuth2Helper = oAuth2Helper;
        this.mongoTransactionManager = mongoTransactionManager;
        this.retryTemplate = retryTemplate;
    }

    @Around(value = "orderLogActionMethodBroaderMatch(orderLogAction, client, id)", argNames = "proceedingJoinPoint,orderLogAction,client,id")
    public Object handleOrderLogActionMethod(ProceedingJoinPoint proceedingJoinPoint,
                                             OrderLogAction orderLogAction,
                                             Client client,
                                             String id) throws Throwable {

        return retryTemplate.execute(retry -> new TransactionTemplate(mongoTransactionManager).execute(a -> {
            try {

                final Order orderBeforeChange = orderService.getOrder(id);
                final Object result = proceedingJoinPoint.proceed();
                LOGGER.debug("Returns: {}", result);

                final Order orderAfterChange = orderService.getOrder(id);
                final String orderLogActionName = resolveOrderLogActionName(orderLogAction, proceedingJoinPoint.getSignature());
                final String who = this.resolvePrincipal(client);
                final OrderLogInformation orderLogInformation = new OrderLogInformation(orderBeforeChange, orderAfterChange);

                final OrderLogChangeObject orderLogChangeObject = Arrays.stream(proceedingJoinPoint.getArgs())
                        .filter(arg -> arg instanceof OrderLogChangeObject)
                        .map(arg -> (OrderLogChangeObject) arg).findFirst().orElse(null);
                orderLogInformation.setOrderLogChangeObject(orderLogChangeObject);

                if (orderLogChangeObject == null) {
                    final MethodSignature signature = (MethodSignature) proceedingJoinPoint.getSignature();
                    final String[] argNames = signature.getParameterNames();
                    Object[] values = proceedingJoinPoint.getArgs();
                    Map<String, Object> orderLogParams = new HashMap<>();

                    for (int i = 0; i < values.length; i++) {
                        final Parameter parameter = signature.getMethod().getParameters()[i];

                        if (parameter.getAnnotation(OrderLogParam.class) != null) {
                            orderLogParams.put(argNames[i], values[i]);
                        }
                    }

                    orderLogInformation.setOrderLogParams(orderLogParams);
                }

                this.createOrderLog(orderLogActionName, who, orderLogInformation);

                return result;


            } catch (Throwable e) {
                LOGGER.debug("Caught an exception: {}", e.getMessage(), e);

                throw new TransactionUsageException(e.getMessage(), e);
            }
        }));
    }

    private String resolveOrderLogActionName(final OrderLogAction orderLogAction, final Signature signature) {

        if (StringUtils.isNotBlank(orderLogAction.value())) {
            return orderLogAction.value();
        }

        return signature.getName();
    }

    private void createOrderLog(String orderLogAction, String who, OrderLogInformation orderLogInformation) {

        LOGGER.info("Order operation succeeded, proceed with creating OrderLog.");
        final OrderLog orderLog = new OrderLog(new Date(), who, orderLogAction);

        final Order orderAfterChange = orderLogInformation.getOrderAfterChange();

        if (orderLogInformation.getOrderLogChangeObject() != null) {
            orderLogInformation.getOrderLogChangeObject().populateOrderLogEntries(orderLogInformation.getOrderBeforeChange(), orderAfterChange, orderLog);
        }

        final Map<String, Object> orderLogParams = orderLogInformation.getOrderLogParams();

        if (orderLogParams != null && orderLogParams.size() > 0) {
            orderLogParams.forEach((k, v) -> orderLog.addOrderLogEntry(k, v.toString()));
        }

        LOGGER.info("Created OrderLog: {}", orderLog);

        orderAfterChange.addOrderLog(orderLog);
        orderService.saveOrder(orderAfterChange);
    }

    private String resolvePrincipal(Client client) {
        final ClientUser clientUser = oAuth2Helper.resolveCurrentClientUser(client);
        return clientUser.getName();
    }

    @Pointcut(value = "@annotation(orderLogAction) && args(client, id, ..)", argNames = "orderLogAction,client,id")
    public void orderLogActionMethodBroaderMatch(OrderLogAction orderLogAction, Client client, String id) {

    }

    @Data
    @RequiredArgsConstructor
    private static class OrderLogInformation {

        private final Order orderBeforeChange;

        private final Order orderAfterChange;

        private OrderLogChangeObject orderLogChangeObject;

        private Map<String, Object> orderLogParams;

    }
}
