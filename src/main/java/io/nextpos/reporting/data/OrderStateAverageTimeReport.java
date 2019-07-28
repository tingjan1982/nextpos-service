package io.nextpos.reporting.data;

import io.nextpos.ordermanagement.data.Order;
import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class OrderStateAverageTimeReport {

    @Id
    private String id;

    private Order.OrderState fromState;

    private Order.OrderState toState;

    private double averageWaitTime;
}
