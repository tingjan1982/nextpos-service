package io.nextpos.ordermanagement.service;

import io.nextpos.ordermanagement.data.Shift;

import java.math.BigDecimal;

public interface ShiftService {

    Shift openShift(String clientId, BigDecimal openingBalance);

    Shift closeShift(String clientId, BigDecimal closingBalance);

    Shift getActiveShift(String clientId);


}
