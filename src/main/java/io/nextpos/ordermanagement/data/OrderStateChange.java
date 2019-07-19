package io.nextpos.ordermanagement.data;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document
@Data
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class OrderStateChange {

    @Id
    private String orderId;

    @Transient
    private OrderStateChangeEntry lastEntry;

    private List<OrderStateChangeEntry> stateChanges = new ArrayList<>();

    public OrderStateChange(final String orderId) {
        this.orderId = orderId;
    }

    public void addStateChange(Order.OrderState fromState, Order.OrderState toState) {

        final OrderStateChangeEntry entry = new OrderStateChangeEntry(fromState, toState, Instant.now());
        stateChanges.add(entry);

        lastEntry = entry;
    }

    @Data
    @NoArgsConstructor(access = AccessLevel.PACKAGE)
    @AllArgsConstructor
    public static class OrderStateChangeEntry {

        private Order.OrderState fromState;

        private Order.OrderState toState;

        private Instant timestamp;
    }
}
