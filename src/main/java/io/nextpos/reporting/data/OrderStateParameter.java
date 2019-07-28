package io.nextpos.reporting.data;

import io.nextpos.ordermanagement.data.Order;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderStateParameter {

    private ReportDateParameter dateParameter;

    private Order.OrderState fromState;

    private Order.OrderState toState;
}
