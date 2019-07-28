package io.nextpos.reporting.web.model;

import io.nextpos.ordermanagement.data.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStateAverageTimeReportResponse {

    private String clientId;

    private LocalDateTime fromDate;

    private LocalDateTime toDate;

    private Order.OrderState fromState;

    private Order.OrderState toState;

    private double averageTimeInSeconds;
}
