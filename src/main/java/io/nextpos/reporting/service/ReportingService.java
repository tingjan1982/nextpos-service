package io.nextpos.reporting.service;

import io.nextpos.client.data.Client;
import io.nextpos.reporting.data.OrderStateAverageTimeReport;
import io.nextpos.reporting.data.OrderStateParameter;

public interface ReportingService {

    OrderStateAverageTimeReport generateStateTransitionAverageTimeReport(Client client, OrderStateParameter orderStateParameter);
}
