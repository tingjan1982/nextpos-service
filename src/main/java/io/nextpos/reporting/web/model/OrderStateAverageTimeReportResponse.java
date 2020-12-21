package io.nextpos.reporting.web.model;

import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.ordermanagement.data.Order;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderStateAverageTimeReportResponse {

    // todo: is this used?
    private String clientId;

    private ZonedDateRange dateRange;

    private Order.OrderState fromState;

    private Order.OrderState toState;

    private double averageTimeInSeconds;
}
