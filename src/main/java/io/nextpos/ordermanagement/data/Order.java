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

        state = OPEN;
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

        OPEN,
        IN_PROCESS,
        PARTIALLY_DELIVERED,
        DELIVERED,
        SETTLED,
        CANCELLED,
        REFUNDED,
        DELETED,
        ANY
    }

    public enum OrderAction {

        SUBMIT(OPEN, IN_PROCESS),
        DELIVER(IN_PROCESS, DELIVERED),
        SETTLE(DELIVERED, SETTLED),
        CANCEL(ANY, CANCELLED),
        REFUND(SETTLED, REFUNDED);

        private final OrderState validStartState;

        private final OrderState validNextState;


        OrderAction(final OrderState validStartState, final OrderState validNextState) {
            this.validStartState = validStartState;
            this.validNextState = validNextState;
        }

        public OrderState getValidStartState() {
            return validStartState;
        }

        public OrderState getValidNextState() {
            return validNextState;
        }
    }

}
