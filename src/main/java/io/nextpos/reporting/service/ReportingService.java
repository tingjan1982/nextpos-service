package io.nextpos.reporting.service;

import io.nextpos.client.data.Client;
import io.nextpos.reporting.data.OrderStateAverageTimeReport;
import io.nextpos.reporting.data.OrderStateParameter;
import io.nextpos.reporting.data.ReportDateParameter;
import io.nextpos.reporting.data.SalesReport;

public interface ReportingService {

    SalesReport generateSalesReport(Client client, ReportDateParameter reportDateParameter);

    OrderStateAverageTimeReport generateStateTransitionAverageTimeReport(Client client, OrderStateParameter orderStateParameter);
}
