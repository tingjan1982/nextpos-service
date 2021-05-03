package io.nextpos.ordertransaction.service;

import io.nextpos.client.data.Client;
import io.nextpos.datetime.service.ZonedDateRangeBuilder;
import io.nextpos.einvoice.common.invoice.ElectronicInvoice;
import io.nextpos.einvoice.common.invoice.ElectronicInvoiceRepository;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderRepository;
import io.nextpos.ordermanagement.data.OrderSettings;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.ordertransaction.data.OrderTransaction;
import io.nextpos.reporting.data.DateParameterType;
import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.exception.BusinessLogicException;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class OrderTransactionServiceImplTest {

    @Autowired
    private OrderTransactionService orderTransactionService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ElectronicInvoiceRepository electronicInvoiceRepository;

    @Autowired
    private Client client;

    @Autowired
    private OrderSettings orderSettings;

    @AfterEach
    void cleanup() {
        orderRepository.deleteAll();
    }

    @Test
    @ChainedTransaction
    void createSingleOrderTransaction() {

        final String clientId = client.getId();
        final Order order = new Order(clientId, orderSettings);
        order.addOrderLineItem(DummyObjects.productSnapshot("coffee", new BigDecimal("50")), 1);
        order.addOrderLineItem(DummyObjects.productSnapshot("sandwich", new BigDecimal("100")), 1);
        order.setState(Order.OrderState.DELIVERED);
        orderService.createOrder(order);

        final OrderTransaction orderTransaction = new OrderTransaction(order, OrderTransaction.PaymentMethod.CARD, OrderTransaction.BillType.SINGLE, order.getOrderTotal());

        orderTransactionService.createOrderTransaction(client, orderTransaction);

        assertThat(orderTransaction.getId()).isNotNull();

        final Order retrievedOrder = orderService.getOrder(orderTransaction.getOrderId());

        assertThat(retrievedOrder.getState()).isEqualTo(Order.OrderState.SETTLED);

        assertThatThrownBy(() -> orderTransactionService.createOrderTransaction(client, orderTransaction)).isInstanceOf(BusinessLogicException.class);
    }

    @Test
    @ChainedTransaction
    void createZeroDollarOrderTransaction() {

        client.addAttribute(Client.ClientAttributes.UBN, "83515813");
        client.addAttribute(Client.ClientAttributes.COMPANY_NAME, "Ron");
        client.addAttribute(Client.ClientAttributes.ADDRESS, "Taipei");
        client.addAttribute(Client.ClientAttributes.AES_KEY, "12341234123412341234123412341234");


        final Order order = Order.newOrder(client.getId(), Order.OrderType.TAKE_OUT, orderSettings);
        order.addOrderLineItem(DummyObjects.productSnapshot("coffee", new BigDecimal("0")), 1);
        order.setState(Order.OrderState.DELIVERED);
        orderService.createOrder(order);

        final OrderTransaction orderTransaction = new OrderTransaction(order, OrderTransaction.PaymentMethod.CARD, OrderTransaction.BillType.SINGLE, order.getOrderTotal());

        orderTransactionService.createOrderTransaction(client, orderTransaction);

        assertThat(orderTransaction.hasElectronicInvoice()).isFalse();


    }

    /**
     * https://www.baeldung.com/java-start-thread
     */
    @Test
    void ensureOneTransactionIsCreated() throws Exception {

        final Order order = createMockOrder();

        ExecutorService executor = Executors.newFixedThreadPool(1);

        final Callable<String> task = () -> {
            final OrderTransaction orderTransaction = new OrderTransaction(order,
                    OrderTransaction.PaymentMethod.CARD,
                    OrderTransaction.BillType.SINGLE,
                    order.getOrderTotal());

            orderTransactionService.createOrderTransaction(client, orderTransaction);

            return "completed";
        };

        executor.invokeAll(Arrays.asList(task, task, task, task, task));

        assertThat(orderTransactionService.getOrderTransactionByOrderId(order.getId())).hasSize(1);
    }

    @Test
    @ChainedTransaction
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

    @Test
    @ChainedTransaction
    void getOrderByInvoiceNumber() {

        final Order order = createMockOrder();
        final OrderTransaction orderTransaction = new OrderTransaction(order, OrderTransaction.PaymentMethod.CASH, OrderTransaction.BillType.SINGLE, order.getOrderTotal());
        final ElectronicInvoice electronicInvoice = createMockElectronicInvoice(order);
        orderTransaction.getInvoiceDetails().setElectronicInvoice(electronicInvoice);
        orderTransactionService.createOrderTransaction(client, orderTransaction);

        assertThatCode(() -> orderTransactionService.getOrderByInvoiceNumber(electronicInvoice.getInternalInvoiceNumber())).doesNotThrowAnyException();
    }

    @Test
    @ChainedTransaction
    void getCancellableOrders() {

        assertThat(orderTransactionService.getCancellableOrders(client, ZonedDateRangeBuilder.builder(client, DateParameterType.MONTH).build())).isEmpty();

        final Order order = createMockOrder();
        final OrderTransaction orderTransaction = new OrderTransaction(order, OrderTransaction.PaymentMethod.CASH, OrderTransaction.BillType.SINGLE, order.getOrderTotal());
        final ElectronicInvoice electronicInvoice = createMockElectronicInvoice(order);
        orderTransaction.getInvoiceDetails().setElectronicInvoice(electronicInvoice);
        orderTransactionService.createOrderTransaction(client, orderTransaction);
        orderService.performOrderAction(order.getId(), Order.OrderAction.DELETE);

        final List<Order> cancellableOrders = orderTransactionService.getCancellableOrders(client, ZonedDateRangeBuilder.builder(client, DateParameterType.MONTH).build());

        assertThat(cancellableOrders).isNotEmpty();
    }

    private ElectronicInvoice createMockElectronicInvoice(Order order) {

        final ElectronicInvoice electronicInvoice = new ElectronicInvoice(client.getId(),
                order.getId(),
                "AA-10001001",
                ElectronicInvoice.InvoiceStatus.CREATED,
                new ElectronicInvoice.InvoicePeriod(ZoneId.systemDefault()),
                new BigDecimal("150"),
                new BigDecimal("8"),
                "83515813",
                "Rain",
                "Main Road",
                List.of());
        return electronicInvoiceRepository.save(electronicInvoice);
    }

    private Order createMockOrder() {
        final Order order = new Order(client.getId(), orderSettings);
        order.setOrderTotal(BigDecimal.valueOf(150));
        order.setState(Order.OrderState.DELIVERED);
        orderService.createOrder(order);

        return order;
    }
}