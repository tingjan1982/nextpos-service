package io.nextpos.reporting.service;

import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.reporting.data.RangedSalesReport;
import io.nextpos.reporting.data.SalesDistribution;
import io.nextpos.reporting.data.SalesProgress;

import java.time.LocalDate;
import java.time.ZoneId;

public interface SalesReportService {

    RangedSalesReport generateRangedSalesReport(String clientId, ZonedDateRange zonedDateRange);

    RangedSalesReport generateSalesRankingReport(String clientId, ZonedDateRange zonedDateRange, String labelId);

    SalesProgress generateSalesProgress(String clientId);

    SalesDistribution generateSalesDistribution(String clientId, final ZoneId zoneId, LocalDate dateFilter);
}
