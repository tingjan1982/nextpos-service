package io.nextpos.reporting.service;

import io.nextpos.reporting.data.CustomerStatsReport;

import java.time.LocalDate;

public interface StatsReportService {

    CustomerStatsReport generateCustomerStatsReport(String clientId, LocalDate dateFilter);
}
