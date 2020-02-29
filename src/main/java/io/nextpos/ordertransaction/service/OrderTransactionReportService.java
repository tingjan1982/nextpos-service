package io.nextpos.ordertransaction.service;

import io.nextpos.ordermanagement.data.Shift;
import io.nextpos.ordertransaction.data.ClosingShiftTransactionReport;

public interface OrderTransactionReportService {

    ClosingShiftTransactionReport getClosingShiftTransactionReport(Shift shift);
}
