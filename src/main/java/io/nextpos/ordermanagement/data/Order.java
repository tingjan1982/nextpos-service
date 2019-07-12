package io.nextpos.ordermanagement.data;

import io.nextpos.shared.model.BaseObject;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static io.nextpos.ordermanagement.data.Order.OrderState.*;

/**
 * MongoDB behavior on entity object:
 * https://docs.spring.io/spring-data/mongodb/docs/2.1.9.RELEASE/reference/html/#mongo-template.save-update-remove
 */
@Document
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class Order extends BaseObject {

    @Id
    private String id;

    private String clientId;

    private OrderState state;

    private List<OrderLineItem> orderLineItems = new ArrayList<>();

    private TaxableAmount total;

    public Order(final String clientId, BigDecimal taxRate) {
        this.clientId = clientId;

        state = NEW;
        total = new TaxableAmount(taxRate);
    }

    public Order addOrderLineItem(OrderLineItem orderLineItem) {
        orderLineItems.add(orderLineItem);

        final BigDecimal lineItemsTotal = orderLineItems.stream()
                .map(li -> li.getSubTotal().getAmountWithoutTax())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        total.calculate(lineItemsTotal);
        
        return this;
    }

    public enum OrderState {

        NEW,
        OPEN,
        PARTIALLY_DELIVERED,
        DELIVERED,
        SETTLED,
        CANCELLED,
        REFUNDED,
        DELETED,
        ANY
    }

    public enum OrderAction {

        SUBMIT(NEW, List.of(OPEN)),
        DELIVER(OPEN, List.of(PARTIALLY_DELIVERED, DELIVERED)),
        SETTLE(DELIVERED, List.of(SETTLED)),
        CANCEL(ANY, List.of(CANCELLED)),
        REFUND(SETTLED, List.of(REFUNDED));

        private final OrderState validStartState;

        private final List<OrderState> validNextState;


        OrderAction(final OrderState validStartState, final List<OrderState> validNextState) {
            this.validStartState = validStartState;
            this.validNextState = validNextState;
        }

        public OrderState getValidStartState() {
            return validStartState;
        }

        public List<OrderState> getValidNextState() {
            return validNextState;
        }
    }

}
