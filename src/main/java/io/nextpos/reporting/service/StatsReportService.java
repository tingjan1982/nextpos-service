package io.nextpos.reporting.service;

import io.nextpos.reporting.data.CustomerCountReport;

import java.time.LocalDate;

public interface StatsReportService {

    CustomerCountReport generateCustomerCountReport(String clientId, LocalDate dateFilter);
}
