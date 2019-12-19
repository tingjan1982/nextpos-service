package io.nextpos.reporting.service;

import io.nextpos.reporting.data.SalesDistribution;
import io.nextpos.reporting.data.SalesProgress;

public interface SalesReportService {

    SalesProgress generateSalesProgress(String clientId);

    SalesDistribution generateSalesDistribution(String clientId);
}
