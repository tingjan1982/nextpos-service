package io.nextpos.reporting.service;

import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.reporting.data.CustomerStatsReport;
import io.nextpos.reporting.data.CustomerTrafficReport;

import java.time.YearMonth;

public interface StatsReportService {

    CustomerTrafficReport generateCustomerTrafficReport(String clientId, ZonedDateRange dateFilter);

    CustomerStatsReport generateCustomerStatsReport(String clientId, YearMonth dateFilter);
}
