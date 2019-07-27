package io.nextpos.reporting.data;

import io.nextpos.ordermanagement.data.Order;
import lombok.Data;

@Data
public class OrderStateElapsedTimeReport {

    private String id;

    private Order.OrderState fromState;

    private Order.OrderState toState;

    private double averageWaitTime;
}
