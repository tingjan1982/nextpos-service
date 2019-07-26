package io.nextpos.reporting.service;

import io.nextpos.client.data.Client;
import io.nextpos.reporting.data.ReportingParameter;
import io.nextpos.reporting.data.SalesReport;

public interface ReportingService {

    SalesReport generateSalesReport(Client client, ReportingParameter reportingParameter);
}
