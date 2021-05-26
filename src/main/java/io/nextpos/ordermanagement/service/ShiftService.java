package io.nextpos.ordermanagement.service;

import io.nextpos.client.data.Client;
import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.notification.data.NotificationDetails;
import io.nextpos.ordermanagement.data.Shift;
import io.nextpos.ordertransaction.data.ClosingShiftTransactionReport;
import io.nextpos.settings.data.PaymentMethod;
import io.nextpos.workingarea.data.SinglePrintInstruction;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface ShiftService {

    Shift openShift(String clientId, BigDecimal openingBalance);

    void saveShift(Shift shift);

    ClosingShiftTransactionReport getClosingShiftReport(String clientId, String shiftId);

    Shift balanceClosingShift(String shiftId);

    Shift initiateCloseShift(String clientId, Set<PaymentMethod> supportedPaymentMethods);

    Shift closeShift(String clientId, Map<String, Shift.ClosingBalanceDetails> closingBalances);

    Shift confirmCloseShift(String clientId, String closingRemark);

    Shift abortCloseShift(String clientId);

    Optional<Shift> getActiveShift(String clientId);

    Optional<Shift> getMostRecentShift(String clientId);

    Shift getActiveShiftOrThrows(String clientId);

    Shift getShift(String shiftId);

    List<Shift> getShifts(String clientId, ZonedDateRange date);

    CompletableFuture<NotificationDetails> sendShiftReport(Client client, String shiftId, String emailAddress);

    SinglePrintInstruction printShiftReport(Client client, String shiftId);
}
