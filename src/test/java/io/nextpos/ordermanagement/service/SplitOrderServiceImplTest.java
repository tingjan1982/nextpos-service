package io.nextpos.ordermanagement.service;

import io.nextpos.client.data.Client;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.ordermanagement.data.OrderSettings;
import io.nextpos.ordermanagement.data.SplitAmountDetails;
import io.nextpos.ordertransaction.data.OrderTransaction;
import io.nextpos.ordertransaction.service.OrderTransactionService;
import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.exception.ObjectNotFoundException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class SplitOrderServiceImplTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SplitOrderServiceImplTest.class);

    private final SplitOrderService splitOrderService;

    private final OrderService orderService;

    private final OrderTransactionService orderTransactionService;

    private final Client client;

    private final OrderSettings orderSettings;

    @Autowired
    SplitOrderServiceImplTest(SplitOrderService splitOrderService, OrderService orderService, OrderTransactionService orderTransactionService, Client client, OrderSettings orderSettings) {
        this.splitOrderService = splitOrderService;
        this.orderService = orderService;
        this.orderTransactionService = orderTransactionService;
        this.client = client;
        this.orderSettings = orderSettings;
    }

    @Test
    void newSplitOrder() {

        final Order sourceOrder = new Order(client.getId(), orderSettings);
        sourceOrder.setState(Order.OrderState.DELIVERED);
        sourceOrder.addOrderLineItem(DummyObjects.productSnapshot(), 3);
        orderService.saveOrder(sourceOrder);

        final OrderLineItem sourceLineItem = sourceOrder.getOrderLineItems().get(0);

        final Order targetOrder = splitOrderService.newSplitOrder(sourceOrder.getId(), sourceLineItem.getId());

        assertThat(targetOrder).satisfies(o -> {
            assertThat(o.getSerialId()).isNotNull();
            assertThat(o.getOneTableInfo().getDisplayName()).isEqualTo("splitOrder");
            assertThat(o.getState()).isEqualByComparingTo(Order.OrderState.DELIVERED);
            assertThat(o.getOrderTotal()).isNotZero();
            assertThat(o.getOrderLineItems()).hasSize(1);
            assertThat(o.getOrderLineItem(sourceLineItem.getId())).satisfies(li -> {
                assertThat(li).isNotNull();
                assertThat(li.getQuantity()).isEqualTo(1);
                assertThat(li.getLineItemSubTotal()).isEqualByComparingTo("100");
            });
        });

        assertThat(orderService.getOrder(sourceOrder.getId())).satisfies(o -> {
            assertThat(o.getOrderLineItems().get(0).getQuantity()).isEqualTo(2);
        });

        final Order updatedOrder = splitOrderService.updateLineItem(targetOrder.getId(), sourceOrder.getId(), sourceLineItem.getId());

        assertThat(updatedOrder.getOrderLineItem(sourceLineItem.getId())).satisfies(li -> {
            assertThat(li).isNotNull();
            assertThat(li.getQuantity()).isEqualTo(3);
        });

        assertThat(orderService.getOrder(targetOrder.getId()).getOrderLineItems()).isEmpty();
    }

    @Test
    void revertSplitOrderLineItems() {

        final Order sourceOrder = new Order(client.getId(), orderSettings);
        sourceOrder.addOrderLineItem(DummyObjects.productSnapshot(), 3);
        sourceOrder.addOrderLineItem(DummyObjects.productSnapshot(), 1);
        orderService.saveOrder(sourceOrder);

        final OrderLineItem sourceLineItem = sourceOrder.getOrderLineItems().get(0);
        final OrderLineItem sourceLineItem2 = sourceOrder.getOrderLineItems().get(1);

        final Order newSplitOrder = splitOrderService.newSplitOrder(sourceOrder.getId(), sourceLineItem.getId());
        splitOrderService.updateLineItem(sourceOrder.getId(), newSplitOrder.getId(), sourceLineItem2.getId());

        splitOrderService.revertSplitOrderLineItems(newSplitOrder.getId(), sourceOrder.getId());

        assertThat(orderService.getOrder(sourceOrder.getId())).satisfies(o -> {
            assertThat(o).isNotNull();
            assertThat(o.getOrderLineItems()).hasSize(2);
            assertThat(o.getOrderLineItem(sourceLineItem.getId())).satisfies(li -> assertThat(li.getQuantity()).isEqualTo(3));
            assertThat(o.getOrderLineItem(sourceLineItem2.getId())).satisfies(li -> assertThat(li.getQuantity()).isEqualTo(1));
        });

        assertThatThrownBy(() -> orderService.getOrder(newSplitOrder.getId())).isInstanceOf(ObjectNotFoundException.class);
    }

    @Test
    void splitByHeadCount() {

        final Order sourceOrder = new Order(client.getId(), orderSettings);
        sourceOrder.addOrderLineItem(DummyObjects.productSnapshot(), 3);
        sourceOrder.setState(Order.OrderState.DELIVERED);
        orderService.saveOrder(sourceOrder);

        LOGGER.info("{}", sourceOrder.getOrderTotal());

        final List<SplitAmountDetails> splitAmountDetails = splitOrderService.splitByHeadCount(sourceOrder.getId(), 3);

        LOGGER.info("{}", splitAmountDetails);

        assertThat(splitAmountDetails).hasSize(3);
        final BigDecimal splitAmountSum = splitAmountDetails.stream().map(SplitAmountDetails::getSplitAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(splitAmountSum).isEqualByComparingTo(sourceOrder.getOrderTotal());

        createOrderTransaction(sourceOrder, new BigDecimal("115"));

        final List<SplitAmountDetails> updatedSplitAmountDetails = splitOrderService.splitByHeadCount(sourceOrder.getId(), 3);

        LOGGER.info("{}", updatedSplitAmountDetails);

        assertThat(updatedSplitAmountDetails).hasSize(3);
        assertThat(updatedSplitAmountDetails).anySatisfy(d -> assertThat(d.isPaid()).isTrue());

        final List<SplitAmountDetails> changedSplitAmountDetails = splitOrderService.splitByHeadCount(sourceOrder.getId(), 1);
        assertThat(changedSplitAmountDetails).hasSize(2);

        LOGGER.info("{}", changedSplitAmountDetails);

        createOrderTransaction(sourceOrder, new BigDecimal("231.50"));

        assertThat(splitOrderService.splitByHeadCount(sourceOrder.getId(), 1)).hasSize(2);
        assertThat(splitOrderService.splitByHeadCount(sourceOrder.getId(), 1)).allSatisfy(d -> assertThat(d.isPaid()).isTrue());
    }

    private void createOrderTransaction(Order sourceOrder, BigDecimal settleAmount) {
        OrderTransaction orderTransaction = new OrderTransaction(
                sourceOrder.getId(),
                client.getId(),
                sourceOrder.getOrderTotal(),
                settleAmount,
                OrderTransaction.PaymentMethod.CARD,
                OrderTransaction.BillType.SPLIT,
                List.of(new OrderTransaction.BillLineItem("split", 1, settleAmount, settleAmount))
        );

        orderTransactionService.createOrderTransaction(client, orderTransaction);
    }
}