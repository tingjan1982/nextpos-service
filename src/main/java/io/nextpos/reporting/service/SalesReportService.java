package io.nextpos.reporting.service;

import io.nextpos.reporting.data.RangedSalesReport;
import io.nextpos.reporting.data.SalesDistribution;
import io.nextpos.reporting.data.SalesProgress;

import java.time.LocalDate;

public interface SalesReportService {

    RangedSalesReport generateWeeklySalesReport(String clientId, final RangedSalesReport.RangeType rangeType, final LocalDate date);

    SalesProgress generateSalesProgress(String clientId);

    SalesDistribution generateSalesDistribution(String clientId, final LocalDate dateFilter);
}
