package io.nextpos.ordermanagement.service;

import io.nextpos.ordermanagement.data.Shift;
import io.nextpos.shared.exception.GeneralApplicationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;

import javax.transaction.Transactional;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@TestPropertySource(properties = "nomock=true")
class ShiftServiceImplTest {

    @Autowired
    private ShiftService shiftService;

    @Test
    @WithMockUser("dummyUser")
    void openAndCloseShift() {

        final String clientId = "dummyClient";

        final Shift openedShift = shiftService.openShift(clientId, BigDecimal.valueOf(1000));

        assertThat(openedShift).satisfies(s -> {
            assertThat(s.getId()).isNotNull();
            assertThat(s.getStart().getWho()).isEqualTo("dummyUser");
            assertThat(s.getStart().getBalance()).isEqualTo(BigDecimal.valueOf(1000));
            assertThat(s.getShiftStatus()).isEqualTo(Shift.ShiftStatus.ACTIVE);
        });

        // there should be an active shift.
        assertThatCode(() -> shiftService.getActiveShift(clientId)).doesNotThrowAnyException();

        final Shift closedShift = shiftService.closeShift(clientId, BigDecimal.valueOf(1000));

        assertThat(closedShift).satisfies(s -> {
            assertThat(s.getStart().getWho()).isEqualTo("dummyUser");
            assertThat(s.getStart().getBalance()).isEqualTo(BigDecimal.valueOf(1000));
            assertThat(s.getShiftStatus()).isEqualTo(Shift.ShiftStatus.BALANCED);
        });

        assertThatThrownBy(() -> shiftService.closeShift(clientId, BigDecimal.valueOf(1000))).isInstanceOf(GeneralApplicationException.class);
    }
}