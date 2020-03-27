package io.nextpos.reporting.service;

import io.nextpos.reporting.data.CustomerStatsReport;

import java.time.YearMonth;

public interface StatsReportService {

    CustomerStatsReport generateCustomerStatsReport(String clientId, YearMonth dateFilter);
}
