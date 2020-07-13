package io.nextpos.ordermanagement.service;

import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.ordermanagement.data.Shift;
import io.nextpos.ordertransaction.data.ClosingShiftTransactionReport;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ShiftService {

    Shift openShift(String clientId, BigDecimal openingBalance);

    ClosingShiftTransactionReport getClosingShiftReport(String clientId, String shiftId);

    Shift balanceClosingShift(String shiftId);

    Shift initiateCloseShift(String clientId);

    Shift closeShift(String clientId, Shift.ClosingBalanceDetails cash, final Shift.ClosingBalanceDetails card);

    Shift confirmCloseShift(String clientId, String closingRemark);

    Shift abortCloseShift(String clientId);

    Optional<Shift> getActiveShift(String clientId);

    Optional<Shift> getMostRecentShift(String clientId);

    Shift getActiveShiftOrThrows(String clientId);

    Shift getShift(String shiftId);

    List<Shift> getShifts(String clientId, ZonedDateRange date);

}
