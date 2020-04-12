package io.nextpos.ordermanagement.service;

import io.nextpos.ordermanagement.data.Shift;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;

public interface ShiftService {

    Shift openShift(String clientId, BigDecimal openingBalance);

    Shift initiateCloseShift(String clientId);

    Shift closeShift(String clientId, Shift.ClosingBalanceDetails cash, final Shift.ClosingBalanceDetails card);

    Shift confirmCloseShift(String clientId, String closingRemark);

    Shift abortCloseShift(String clientId);

    Optional<Shift> getActiveShift(String clientId);

    Optional<Shift> getMostRecentShift(String clientId);

    Shift getActiveShiftOrThrows(String clientId);

    Shift getShift(String shiftId);

    Page<Shift> getShifts(String clientId, Date date, PageRequest pageRequest);

}
