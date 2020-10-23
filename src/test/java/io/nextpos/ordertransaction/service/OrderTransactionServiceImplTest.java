package io.nextpos.ordertransaction.service;

import io.nextpos.client.data.Client;
import io.nextpos.einvoice.common.invoice.ElectronicInvoice;
import io.nextpos.einvoice.common.invoice.ElectronicInvoiceRepository;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderSettings;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.ordertransaction.data.OrderTransaction;
import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.exception.BusinessLogicException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZoneId;
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
    private ElectronicInvoiceRepository electronicInvoiceRepository;

    @Autowired
    private Client client;

    @Autowired
    private OrderSettings orderSettings;

    @Test
    void createSingleOrderTransaction() {

        final String clientId = client.getId();
        final Order order = new Order(clientId, orderSettings);
        order.addOrderLineItem(DummyObjects.productSnapshot("coffee", new BigDecimal("50")), 1);
        order.addOrderLineItem(DummyObjects.productSnapshot("sandwich", new BigDecimal("100")), 1);
        order.setState(Order.OrderState.DELIVERED);
        orderService.saveOrder(order);

        final OrderTransaction orderTransaction = new OrderTransaction(order.getId(), clientId, order.getOrderTotal(), order.getOrderTotal(),
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

        final Order order = createMockOrder();

        ExecutorService executor = Executors.newFixedThreadPool(5);

        final Callable<String> task = () -> {
            final OrderTransaction orderTransaction = new OrderTransaction(order.getId(), client.getId(), order.getOrderTotal(), BigDecimal.valueOf(200),
                    OrderTransaction.PaymentMethod.CARD,
                    OrderTransaction.BillType.SINGLE,
                    List.of());

            orderTransactionService.createOrderTransaction(client, orderTransaction);

            return "completed";
        };

        executor.invokeAll(Arrays.asList(task, task, task, task, task));

        assertThat(orderTransactionService.getOrderTransactionByOrderId(order.getId())).hasSize(1);
    }

    @Test
    void cancelOrderTransaction() {

        final Order order = createMockOrder();

        final OrderTransaction orderTransaction = new OrderTransaction(order.getId(), client.getId(), order.getOrderTotal(), BigDecimal.valueOf(150),
                OrderTransaction.PaymentMethod.CARD,
                OrderTransaction.BillType.SINGLE,
                List.of());

        final ElectronicInvoice electronicInvoice = createMockElectronicInvoice(order);
        orderTransaction.getInvoiceDetails().setElectronicInvoice(electronicInvoice);
        orderTransactionService.createOrderTransaction(client, orderTransaction);

        orderTransactionService.cancelOrderTransaction(orderTransaction.getId());

        final OrderTransaction updatedTransaction = orderTransactionService.getOrderTransaction(orderTransaction.getId());
        assertThat(updatedTransaction.getStatus()).isEqualByComparingTo(OrderTransaction.OrderTransactionStatus.CANCELLED);

        final Order updatedOrder = orderService.getOrder(order.getId());
        assertThat(updatedOrder.getState()).isEqualByComparingTo(Order.OrderState.CANCELLED);
    }

    private ElectronicInvoice createMockElectronicInvoice(Order order) {

        final ElectronicInvoice electronicInvoice = new ElectronicInvoice(order.getId(),
                "AA-10001001",
                new ElectronicInvoice.InvoicePeriod(ZoneId.systemDefault()),
                new BigDecimal("150"),
                new BigDecimal("8"),
                "83515813",
                "Rain",
                List.of());
        return electronicInvoiceRepository.save(electronicInvoice);
    }

    private Order createMockOrder() {

        final Order order = new Order(client.getId(), orderSettings);
        order.setOrderTotal(BigDecimal.valueOf(150));
        order.setState(Order.OrderState.DELIVERED);
        orderService.saveOrder(order);
        return order;
    }
}