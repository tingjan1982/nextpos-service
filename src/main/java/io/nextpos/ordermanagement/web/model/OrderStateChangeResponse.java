package io.nextpos.ordermanagement.web.model;

import io.nextpos.ordermanagement.data.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStateChangeResponse {

    private String orderId;

    private Order.OrderState fromState;

    private Order.OrderState toState;

    private Instant timestamp;
}
