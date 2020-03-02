package io.nextpos.ordermanagement.data;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Document
@Data
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class OrderStateChange {

    @Id
    private String orderId;

    private String clientId;

    @Transient
    private OrderStateChangeEntry lastEntry;

    private List<OrderStateChangeEntry> stateChanges = new ArrayList<>();

    public OrderStateChange(final String orderId, final String clientId) {
        this.orderId = orderId;
        this.clientId = clientId;
    }

    public void addStateChange(Order.OrderState fromState, Order.OrderState toState) {

        final OrderStateChangeEntry entry = new OrderStateChangeEntry(fromState, toState, Instant.now());
        stateChanges.add(entry);

        lastEntry = entry;
    }

    public OrderDuration getOrderDuration() {

        if (stateChanges.size() < 2) {
            return new OrderDuration();
        }

        final OrderStateChangeEntry firstStateChange = stateChanges.get(0);
        final OrderStateChangeEntry lastStateChange = stateChanges.get(stateChanges.size() - 1);

        final Duration duration = Duration.between(firstStateChange.getTimestamp(), lastStateChange.getTimestamp());

        return new OrderDuration(
                Date.from(firstStateChange.getTimestamp()),
                Date.from(lastStateChange.getTimestamp()),
                duration.toHours(),
                duration.toMinutesPart());
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
