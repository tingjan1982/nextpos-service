package io.nextpos.reporting.web.model;

import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.ordermanagement.data.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStateAverageTimeReportResponse {

    private String clientId;

    private ZonedDateRange dateRange;

    private Order.OrderState fromState;

    private Order.OrderState toState;

    private double averageTimeInSeconds;
}
