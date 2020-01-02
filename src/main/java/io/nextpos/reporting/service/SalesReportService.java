package io.nextpos.reporting.service;

import io.nextpos.reporting.data.RangedSalesReport;
import io.nextpos.reporting.data.SalesDistribution;
import io.nextpos.reporting.data.SalesProgress;

public interface SalesReportService {

    RangedSalesReport generateWeeklySalesReport(String clientId, final RangedSalesReport.RangeType rangeType);

    SalesProgress generateSalesProgress(String clientId);

    SalesDistribution generateSalesDistribution(String clientId);
}
