package io.nextpos.timecard.service;

import io.nextpos.client.data.Client;
import io.nextpos.timecard.data.TimeCardReport;

import java.time.YearMonth;

public interface TimeCardReportService {

    TimeCardReport getTimeCardReport(Client client, YearMonth yearMonth);
}
