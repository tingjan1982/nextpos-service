package io.nextpos.reporting.service;

import io.nextpos.reporting.data.CustomerStatsReport;
import io.nextpos.reporting.data.CustomerTrafficReport;

import java.time.YearMonth;

public interface StatsReportService {

    CustomerTrafficReport generateCustomerTrafficReport(String clientId, YearMonth dateFilter);

    CustomerStatsReport generateCustomerStatsReport(String clientId, YearMonth dateFilter);
}
