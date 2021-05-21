package io.nextpos.ordertransaction.web;

import io.nextpos.client.data.Client;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderSettings;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.ordertransaction.data.OrderTransaction;
import io.nextpos.ordertransaction.service.OrderTransactionService;
import io.nextpos.ordertransaction.web.model.OrderTransactionRequest;
import io.nextpos.ordertransaction.web.model.OrderTransactionResponse;
import io.nextpos.shared.DummyObjects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OrderTransactionControllerTest {

    @Autowired
    private OrderTransactionController controller;

    @Autowired
    private OrderTransactionService orderTransactionService;
            
    @Autowired
    private OrderService orderService;

    @Autowired
    private Client client;

    @Autowired
    private OrderSettings orderSettings;


    @BeforeEach
    void prepare() {
    }

    @Test
    @WithMockUser
    void createOrderTransaction() {

        final String clientId = client.getId();
        final Order order = new Order(clientId, orderSettings);
        order.addOrderLineItem(DummyObjects.productSnapshot("coffee", new BigDecimal("50")), 1);
        order.addOrderLineItem(DummyObjects.productSnapshot("sandwich", new BigDecimal("100")), 1);
        order.applyOffer(new BigDecimal("100"));
        order.setState(Order.OrderState.DELIVERED);
        orderService.createOrder(order);

        OrderTransactionRequest request = new OrderTransactionRequest();
        request.setOrderId(order.getId());
        request.setPaymentMethod("CARD");
        request.setBillType("SINGLE");

        final OrderTransactionResponse response = controller.createOrderTransaction(client, request);

        final Order retrievedOrder = orderService.getOrder(order.getId());
        assertThat(retrievedOrder.getState()).isEqualTo(Order.OrderState.SETTLED);
        
        final OrderTransaction orderTransaction = orderTransactionService.getOrderTransaction(response.getTransactionId());

        assertThat(orderTransaction.getOrderTotal()).isEqualByComparingTo(order.getOrderTotal());
        assertThat(orderTransaction.getSettleAmount()).isEqualByComparingTo(order.getOrderTotal());
        assertThat(orderTransaction.getPaymentDetails().getPaymentMethod()).isEqualTo(OrderTransaction.PaymentMethod.CARD.name());
        assertThat(orderTransaction.getBillDetails()).satisfies(b -> {
            assertThat(b.getBillType()).isEqualByComparingTo(OrderTransaction.BillType.SINGLE);
            assertThat(b.getBillLineItems()).hasSize(4);

        });


    }
}