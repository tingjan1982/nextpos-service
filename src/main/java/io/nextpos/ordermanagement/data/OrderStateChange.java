package io.nextpos.ordermanagement.data;

import io.nextpos.shared.exception.BusinessLogicException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Document
@Data
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class OrderStateChange {

    @Id
    private String orderId;

    private String clientId;

    private List<OrderStateChangeEntry> stateChanges = new ArrayList<>();

    public OrderStateChange(final String orderId, final String clientId) {
        this.orderId = orderId;
        this.clientId = clientId;
    }

    public void addStateChange(Order.OrderState fromState, Order.OrderState toState) {

        final OrderStateChangeEntry entry = new OrderStateChangeEntry(fromState, toState, Instant.now());
        stateChanges.add(entry);
    }

    public Optional<OrderStateChangeEntry> getLastEntry() {
        return stateChanges.isEmpty() ? Optional.empty() : Optional.of(stateChanges.get(0));
    }

    public OrderStateChangeEntry getPreviousEntry() {

        if (stateChanges.isEmpty()) {
            throw new BusinessLogicException("message.noPreviousEntry", "Please check order state change as there is no recorded state change entries");
        }

        return stateChanges.get(stateChanges.size() - 1);
    }

    public Optional<OrderDuration> getOrderPreparationDuration() {

        final Optional<OrderStateChangeEntry> deliveredState = stateChanges.stream().filter(o -> o.toState == Order.OrderState.DELIVERED).findFirst();

        if (deliveredState.isPresent()) {
            final OrderStateChangeEntry firstStateChange = stateChanges.get(0);

            final Duration duration = Duration.between(firstStateChange.getTimestamp(), deliveredState.get().getTimestamp());

            return Optional.of(new OrderDuration(
                    Date.from(firstStateChange.getTimestamp()),
                    Date.from(deliveredState.get().getTimestamp()),
                    duration.toHours(),
                    duration.toMinutesPart()));
        }

        return Optional.empty();
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
