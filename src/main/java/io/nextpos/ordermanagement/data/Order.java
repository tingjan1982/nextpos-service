package io.nextpos.ordermanagement.data;

import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.model.MongoBaseObject;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static io.nextpos.ordermanagement.data.Order.OrderState.*;

/**
 * MongoDB behavior on entity object:
 * https://docs.spring.io/spring-data/mongodb/docs/2.1.9.RELEASE/reference/html/#mongo-template.save-update-remove
 */
@Document
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class Order extends MongoBaseObject {

    @Id
    private String id;

    private String clientId;

    private OrderState state;

    private List<OrderLineItem> orderLineItems = new ArrayList<>();

    private TaxableAmount total;

    private Currency currency;

    private String tableId;

    /**
     * this represents the id suffix of line item id.
     */
    private AtomicInteger internalCounter;

    @Version
    private Long version;

    public Order(final String clientId, BigDecimal taxRate, final Currency currency) {
        this.id = new ObjectId().toString();
        this.clientId = clientId;
        this.state = OPEN;
        this.total = new TaxableAmount(taxRate);
        this.internalCounter = new AtomicInteger(1);
    }

    /**
     * https://jira.spring.io/browse/DATAMONGO-946
     * 
     * @return
     */
    @Override
    public boolean isNew() {
        return version == null;
    }

    public Order addOrderLineItem(OrderLineItem orderLineItem) {

        final String orderLineItemId = this.id + "-" + internalCounter.getAndIncrement();
        orderLineItem.setId(orderLineItemId);
        orderLineItems.add(orderLineItem);

        computeTotal();

        return this;
    }

    /**
     * Quantity of 0 will remove the line item.
     *
     * @param lineItemId
     * @param quantity
     */
    public void updateOrderLineItem(String lineItemId, int quantity) {

        final OrderLineItem orderLineItem = this.getOrderLineItem(lineItemId);

        if (quantity == 0) {
            orderLineItems.remove(orderLineItem);
        } else {
            orderLineItem.updateQuantity(quantity);
        }

        computeTotal();
    }

    public OrderLineItem getOrderLineItem(String lineItemId) {
        return orderLineItems.stream()
                .filter(li -> StringUtils.equals(li.getId(), lineItemId))
                .findFirst().orElseThrow(() -> {
                    throw new ObjectNotFoundException(lineItemId, OrderLineItem.class);
                });
    }

    private void computeTotal() {
        final BigDecimal lineItemsTotal = orderLineItems.stream()
                .map(li -> li.getSubTotal().getAmountWithoutTax())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        total.calculate(lineItemsTotal);
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
    }

    public enum OrderAction {

        DELETE(OPEN, DELETED),
        SUBMIT(OPEN, IN_PROCESS),
        CANCEL(IN_PROCESS, CANCELLED),
        DELIVER(IN_PROCESS, DELIVERED),
        SETTLE(DELIVERED, SETTLED),
        REFUND(SETTLED, REFUNDED);

        private final OrderState validFromState;

        private final OrderState validNextState;


        OrderAction(final OrderState validFromState, final OrderState validNextState) {
            this.validFromState = validFromState;
            this.validNextState = validNextState;
        }

        public OrderState getValidFromState() {
            return validFromState;
        }

        public OrderState getValidNextState() {
            return validNextState;
        }
    }

}
