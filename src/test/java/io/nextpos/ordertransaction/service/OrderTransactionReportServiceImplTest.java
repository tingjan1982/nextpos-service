package io.nextpos.ordertransaction.service;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderSettings;
import io.nextpos.ordermanagement.data.Shift;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.ordertransaction.data.ClosingShiftTransactionReport;
import io.nextpos.ordertransaction.data.OrderTransaction;
import io.nextpos.ordertransaction.data.OrderTransactionRepository;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;

@SpringBootTest
@Transactional
class OrderTransactionReportServiceImplTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderTransactionReportServiceImplTest.class);

    @Autowired
    private OrderTransactionReportService orderTransactionReportService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderTransactionRepository orderTransactionRepository;

    @Autowired
    private OrderSettings orderSettings;

    @Test
    void getClosingShiftTransactionReport() {

        final Order order = createOrder(Order.OrderState.SETTLED);
        createOrderTransaction(order, order.getOrderTotal(), OrderTransaction.PaymentMethod.CARD);

        final Order order2 = createOrder(Order.OrderState.SETTLED);
        createOrderTransaction(order2, order.getOrderTotal(), OrderTransaction.PaymentMethod.CASH);
        createOrderTransaction(order2, order.getOrderTotal(), OrderTransaction.PaymentMethod.CASH);

        createOrder(Order.OrderState.DELETED);

        final Shift shift = new Shift("client", order.getCreatedDate(), "dummy", BigDecimal.ZERO);
        shift.getEnd().setTimestamp(new Date());

        final ClosingShiftTransactionReport result = orderTransactionReportService.getClosingShiftTransactionReport(shift);

        LOGGER.info("{}", result);
    }

    private Order createOrder(final Order.OrderState state) {
        final Order order = new Order("client", orderSettings);
        order.setOrderTotal(BigDecimal.valueOf(99));
        order.setServiceCharge(BigDecimal.valueOf(10));
        order.setDiscount(BigDecimal.valueOf(10));
        order.setState(state);

        return orderService.saveOrder(order);
    }

    private void createOrderTransaction(Order order, BigDecimal settleAmount, OrderTransaction.PaymentMethod paymentMethod) {

        final OrderTransaction transaction = new OrderTransaction(order, paymentMethod, OrderTransaction.BillType.SINGLE, settleAmount);
        orderTransactionRepository.save(transaction);
    }
}