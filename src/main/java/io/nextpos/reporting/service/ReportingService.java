package io.nextpos.reporting.service;

import io.nextpos.client.data.Client;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.reporting.data.OrderStateElapsedTimeReport;
import io.nextpos.reporting.data.ReportingParameter;
import io.nextpos.reporting.data.SalesReport;

public interface ReportingService {

    SalesReport generateSalesReport(Client client, ReportingParameter reportingParameter);

    OrderStateElapsedTimeReport generateOrderStateElapsedTimeReport(Client client, Order.OrderState fromState, Order.OrderState toState);
}
