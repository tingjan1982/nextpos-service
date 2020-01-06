package io.nextpos.ordermanagement.service;

import io.nextpos.ordermanagement.data.Shift;

import java.math.BigDecimal;
import java.util.Optional;

public interface ShiftService {

    Shift openShift(String clientId, BigDecimal openingBalance);

    Shift createInterimBalance(String clientId, BigDecimal interimBalance);

    Shift closeShift(String clientId, BigDecimal closingBalance);

    Optional<Shift> getActiveShift(String clientId);

    Optional<Shift> getMostRecentShift(String clientId);

    Shift getActiveShiftOrThrows(String clientId);
}
