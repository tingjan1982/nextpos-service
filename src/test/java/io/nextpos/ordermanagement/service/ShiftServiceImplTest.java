package io.nextpos.ordermanagement.service;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderSettings;
import io.nextpos.ordermanagement.data.Shift;
import io.nextpos.ordermanagement.data.ShiftRepository;
import io.nextpos.shared.exception.BusinessLogicException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.Date;
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
    private ShiftRepository shiftRepository;

    @Autowired
    private OrderSettings orderSettings;

    private final String clientId = "dummyClient";

    @AfterEach
    void cleanData() {
        shiftRepository.deleteAll();
    }

    @Test
    @WithMockUser("dummyUser")
    void openAndCloseShift() {

        final Shift openedShift = shiftService.openShift(clientId, BigDecimal.valueOf(1000));

        assertThat(openedShift).satisfies(s -> {
            assertThat(s.getId()).isNotNull();
            assertThat(s.getStart().getTimestamp()).isBefore(new Date());
            assertThat(s.getStart().getWho()).isEqualTo("dummyUser");
            assertThat(s.getStart().getBalance()).isEqualTo(BigDecimal.valueOf(1000));
            assertThat(s.getShiftStatus()).isEqualTo(Shift.ShiftStatus.ACTIVE);
        });

        final Order order = new Order(clientId, orderSettings);
        orderService.createOrder(order);

        // there should be an active shift.
        assertThatCode(() -> shiftService.getActiveShift(clientId)).doesNotThrowAnyException();
        assertThatThrownBy(() -> shiftService.initiateCloseShift(clientId)).isInstanceOf(BusinessLogicException.class);

        orderService.deleteOrder(order);

        assertThat(shiftService.initiateCloseShift(clientId)).satisfies(s -> {
            assertThat(s.getEnd().getClosingShiftReport()).isNotNull();
            assertThat(s.getShiftStatus()).isEqualTo(Shift.ShiftStatus.CLOSING);
        });

        final Shift closingShift = shiftService.closeShift(clientId, Shift.ClosingBalanceDetails.of(BigDecimal.valueOf(1000)), Shift.ClosingBalanceDetails.of(BigDecimal.valueOf(2000)));

        assertThat(closingShift).satisfies(s -> {
            assertThat(s.getStart().getWho()).isEqualTo("dummyUser");
            assertThat(s.getEnd().getTimestamp()).isBefore(new Date());
            assertThat(s.getEnd().getClosingBalances()).hasSize(2);
            assertThat(s.getEnd().getClosingShiftReport()).isNotNull();
            assertThat(s.getShiftStatus()).isEqualTo(Shift.ShiftStatus.CONFIRM_CLOSE);
        });

        final Shift closedShift = shiftService.confirmCloseShift(clientId, "closing remark");
        assertThat(closedShift).satisfies(s -> assertThat(s.getEnd().getClosingRemark()).isNotNull());

        final Optional<Shift> mostRecentShift = shiftService.getMostRecentShift(clientId);
        assertThat(mostRecentShift).isPresent();
        assertThat(mostRecentShift).get().isEqualTo(closedShift);
    }

    @Test
    @WithMockUser("dummyUser")
    void abortShift() {

        shiftService.openShift(clientId, BigDecimal.valueOf(1000));
        shiftService.initiateCloseShift(clientId);
        shiftService.closeShift(clientId, Shift.ClosingBalanceDetails.of(BigDecimal.valueOf(100)), null);

        assertThat(shiftService.abortCloseShift(clientId)).satisfies(s -> {
            assertThat(s.getEnd()).isNotNull();
            assertThat(s.getEnd().getClosingShiftReport()).isNull();
            assertThat(s.getEnd().getClosingBalances()).isEmpty();
            assertThat(s.getShiftStatus()).isEqualTo(Shift.ShiftStatus.ACTIVE);
        });
    }
}