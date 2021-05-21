package io.nextpos.ordermanagement.service;

import io.nextpos.client.data.Client;
import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.datetime.service.ZonedDateRangeBuilder;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderSettings;
import io.nextpos.ordermanagement.data.Shift;
import io.nextpos.ordermanagement.data.ShiftRepository;
import io.nextpos.ordertransaction.data.OrderTransaction;
import io.nextpos.ordertransaction.service.OrderTransactionService;
import io.nextpos.reporting.data.DateParameterType;
import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.exception.BusinessLogicException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@TestPropertySource(properties = "nomock=true")
class ShiftServiceImplTest {

    @Autowired
    private ShiftService shiftService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderTransactionService orderTransactionService;

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private OrderSettings orderSettings;

    private final String clientId = "dummyClient";

    private Client client;

    @BeforeEach
    void setup() {
        client = DummyObjects.dummyClient();
        client.setId(clientId);
    }

    @AfterEach
    void cleanData() {
        shiftRepository.deleteAll();
    }

    @Test
    @WithMockUser("dummyUser")
    void openAndCloseShift() throws Exception {

        final Shift openedShift = shiftService.openShift(clientId, BigDecimal.valueOf(1000));

        assertThat(openedShift).satisfies(s -> {
            assertThat(s.getId()).isNotNull();
            assertThat(s.getStart().getTimestamp()).isBefore(new Date());
            assertThat(s.getStart().getWho()).isEqualTo("dummyUser");
            assertThat(s.getStart().getBalance()).isEqualTo(BigDecimal.valueOf(1000));
            assertThat(s.getShiftStatus()).isEqualTo(Shift.ShiftStatus.ACTIVE);
        });

        final Order order = createOrder(OrderTransaction.PaymentMethod.CASH, false);
        orderService.deleteOrderLineItem(order, order.getOrderLineItems().get(0).getId());

        // there should be an active shift.
        assertThatCode(() -> shiftService.getActiveShift(clientId)).doesNotThrowAnyException();
        assertThatThrownBy(() -> shiftService.initiateCloseShift(clientId)).isInstanceOf(BusinessLogicException.class);

        orderService.performOrderAction(order.getId(), Order.OrderAction.DELETE);

        createOrder(OrderTransaction.PaymentMethod.CASH, true);
        createOrder(OrderTransaction.PaymentMethod.CARD, true);

        assertThat(shiftService.initiateCloseShift(clientId)).satisfies(s -> {
            assertThat(s.getEnd().getClosingShiftReport()).isNotNull();
            assertThat(s.getShiftStatus()).isEqualTo(Shift.ShiftStatus.CLOSING);
        });

        final Shift.ClosingBalanceDetails cashClosingBalance = Shift.ClosingBalanceDetails.of(BigDecimal.valueOf(1000));
        cashClosingBalance.setUnbalanceReason("算錯");
        final Shift.ClosingBalanceDetails cardClosingBalance = Shift.ClosingBalanceDetails.of(BigDecimal.valueOf(2000));
        cardClosingBalance.setUnbalanceReason("單不見了");
        final Shift closingShift = shiftService.closeShift(clientId, Shift.createClosingBalances(cashClosingBalance, cardClosingBalance));

        assertThat(closingShift).satisfies(s -> {
            assertThat(s.getStart().getWho()).isEqualTo("dummyUser");
            assertThat(s.getEnd().getTimestamp()).isBefore(new Date());
            assertThat(s.getEnd().getClosingBalances()).hasSize(2);

            assertThat(s.getEnd().getClosingBalances()).allSatisfy((pm, b) -> {
                assertThat(b.getExpectedBalance()).isNotZero();
                assertThat(b.getClosingBalance()).isNotZero();
                assertThat(b.getDifference()).isNotZero();
                assertThat(b.getUnbalanceReason()).isNotBlank();
            });

            assertThat(s.getEnd().getClosingShiftReport()).isNotNull();
            assertThat(s.getShiftStatus()).isEqualTo(Shift.ShiftStatus.CONFIRM_CLOSE);
        });

        final Shift closedShift = shiftService.confirmCloseShift(clientId, "closing remark");
        assertThat(closedShift).satisfies(s -> assertThat(s.getEnd().getClosingRemark()).isNotNull());

        shiftService.sendShiftReport(client, closedShift.getId(), "rain.io.app@gmail.com").get();

        final Optional<Shift> mostRecentShift = shiftService.getMostRecentShift(clientId);
        assertThat(mostRecentShift).isPresent();
        assertThat(mostRecentShift).get().isEqualTo(closedShift);

        final Shift balancedShift = shiftService.balanceClosingShift(closedShift.getId());

        assertThat(balancedShift.getEnd().getClosingBalances()).allSatisfy((pm, b) -> {
            assertThat(b.getExpectedBalance()).isNotZero();
            assertThat(b.getClosingBalance()).isNotZero();
            assertThat(b.getDifference()).isZero();
        });
    }

    private Order createOrder(OrderTransaction.PaymentMethod paymentMethod, boolean settle) {

        final Order order = Order.newOrder(clientId, Order.OrderType.IN_STORE, orderSettings);
        order.addOrderLineItem(DummyObjects.productSnapshot(), 2);
        order.setState(Order.OrderState.DELIVERED);

        orderService.createOrder(order);

        if (settle) {
            orderTransactionService.createOrderTransaction(client, new OrderTransaction(order, paymentMethod, OrderTransaction.BillType.SINGLE, order.getOrderTotal()));
            orderService.performOrderAction(order.getId(), Order.OrderAction.COMPLETE);
        }

        return order;
    }

    @Test
    @WithMockUser("dummyUser")
    void abortShift() {

        shiftService.openShift(clientId, BigDecimal.valueOf(1000));
        shiftService.initiateCloseShift(clientId);
        shiftService.closeShift(clientId, Shift.createClosingBalances(Shift.ClosingBalanceDetails.of(BigDecimal.valueOf(100)), null));

        assertThat(shiftService.abortCloseShift(clientId)).satisfies(s -> {
            assertThat(s.getEnd()).isNotNull();
            assertThat(s.getEnd().getClosingShiftReport()).isNull();
            assertThat(s.getEnd().getClosingBalances()).isEmpty();
            assertThat(s.getShiftStatus()).isEqualTo(Shift.ShiftStatus.ACTIVE);
        });
    }

    @Test
    @WithMockUser
    void listShifts() {
        final Instant now = ZonedDateTime.now().toInstant();

        shiftRepository.save(new Shift(clientId, new Date(), "dummy", BigDecimal.ONE));
        shiftRepository.save(new Shift(clientId, Date.from(now.minus(3, ChronoUnit.DAYS)), "dummy", BigDecimal.ONE));
        shiftRepository.save(new Shift(clientId, Date.from(now.minus(5, ChronoUnit.DAYS)), "dummy", BigDecimal.ONE));
        shiftRepository.save(new Shift(clientId, Date.from(now.minus(7, ChronoUnit.DAYS)), "dummy", BigDecimal.ONE));
        shiftRepository.save(new Shift(clientId, Date.from(now.minus(8, ChronoUnit.DAYS)), "dummy", BigDecimal.ONE));

        final ZonedDateTime zonedNow = ZonedDateTime.now().withZoneSameInstant(client.getZoneId());
        final ZonedDateRange zonedDateRange = ZonedDateRangeBuilder.builder(client, DateParameterType.RANGE)
                .dateRange(zonedNow.minusDays(7).minusSeconds(1).toLocalDateTime(), zonedNow.plusSeconds(1).toLocalDateTime()).build();

        final List<Shift> shifts = shiftService.getShifts(clientId, zonedDateRange);

        assertThat(shifts).hasSize(4);
        final Comparator<Shift> compareByStartDate = Comparator.<Shift, Date>comparing(s -> s.getStart().getTimestamp()).reversed();

        assertThat(shifts).isSortedAccordingTo(compareByStartDate);
    }
}