package io.nextpos.ordertransaction.service;

import io.nextpos.client.data.Client;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderSettings;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.ordertransaction.data.OrderTransaction;
import io.nextpos.shared.exception.BusinessLogicException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class OrderTransactionServiceImplTest {

    @Autowired
    private OrderTransactionService orderTransactionService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private Client client;

    @Autowired
    private OrderSettings orderSettings;

    @Test
    void createOrderTransaction() {

        final String clientId = client.getId();
        final Order order = new Order(clientId, orderSettings);
        order.setOrderTotal(BigDecimal.valueOf(150));
        order.setState(Order.OrderState.DELIVERED);
        orderService.saveOrder(order);

        // 157.0 settle amount is to accommodate the default 5% tax.
        final OrderTransaction orderTransaction = new OrderTransaction(order.getId(), clientId, order.getOrderTotal(), BigDecimal.valueOf(200),
                OrderTransaction.PaymentMethod.CARD,
                OrderTransaction.BillType.SINGLE,
                List.of());

        orderTransactionService.createOrderTransaction(client, orderTransaction);

        assertThat(orderTransaction.getId()).isNotNull();

        final Order retrievedOrder = orderService.getOrder(orderTransaction.getOrderId());

        assertThat(retrievedOrder.getState()).isEqualTo(Order.OrderState.SETTLED);

        assertThatThrownBy(() -> orderTransactionService.createOrderTransaction(client, orderTransaction)).isInstanceOf(BusinessLogicException.class);
    }

    /**
     * https://www.baeldung.com/java-start-thread
     */
    @Test
    void ensureOneTransactionIsCreated() throws Exception {

        final String clientId = client.getId();
        final Order order = new Order(clientId, orderSettings);
        order.setOrderTotal(BigDecimal.valueOf(150));
        order.setState(Order.OrderState.DELIVERED);
        orderService.saveOrder(order);

        ExecutorService executor = Executors.newFixedThreadPool(5);

        final Callable<String> task = () -> {
            final OrderTransaction orderTransaction = new OrderTransaction(order.getId(), clientId, order.getOrderTotal(), BigDecimal.valueOf(200),
                    OrderTransaction.PaymentMethod.CARD,
                    OrderTransaction.BillType.SINGLE,
                    List.of());

            orderTransactionService.createOrderTransaction(client, orderTransaction);

            return "completed";
        };

        executor.invokeAll(Arrays.asList(task, task, task, task, task));

        assertThat(orderTransactionService.getOrderTransactionByOrderId(order.getId())).hasSize(1);
    }
}