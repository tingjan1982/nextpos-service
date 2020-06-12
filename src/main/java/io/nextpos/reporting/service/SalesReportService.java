package io.nextpos.reporting.service;

import io.nextpos.reporting.data.RangedSalesReport;
import io.nextpos.reporting.data.ReportDateParameter;
import io.nextpos.reporting.data.SalesDistribution;
import io.nextpos.reporting.data.SalesProgress;

import java.time.LocalDate;

public interface SalesReportService {

    RangedSalesReport generateRangedSalesReport(String clientId, RangedSalesReport.RangeType rangeType, LocalDate date, ReportDateParameter reportDateParameter);

    SalesProgress generateSalesProgress(String clientId);

    SalesDistribution generateSalesDistribution(String clientId, LocalDate dateFilter);
}
