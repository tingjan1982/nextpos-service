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
import java.util.EnumSet;
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

    private TaxableAmount discountedTotal;

    private BigDecimal serviceCharge  = BigDecimal.ZERO;

    private Currency currency;

    private String tableId;

    private DemographicData demographicData;

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
        this.currency = currency;
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

    /**
     * total + service charge
     *
     * @return
     */
    public BigDecimal getOrderTotal() {

        BigDecimal serviceChargeAmount = BigDecimal.ZERO;

        if (serviceCharge.compareTo(BigDecimal.ZERO) > 0) {
            serviceChargeAmount = total.getAmountWithTax().multiply(serviceCharge);
        }

        return total.getAmountWithTax().add(serviceChargeAmount);

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

    public void computeTotal() {
        final BigDecimal lineItemsTotal = orderLineItems.stream()
                .map(li -> li.getSubTotal().getAmountWithoutTax())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        total.calculate(lineItemsTotal);

        this.computeDiscountedTotal();
    }

    private void computeDiscountedTotal() {

        final BigDecimal lineItemsTotal = orderLineItems.stream()
                .map(li -> li.getDiscountedSubTotal() != null ? li.getDiscountedSubTotal() : li.getSubTotal())
                .map(TaxableAmount::getAmountWithoutTax)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.applyDiscountedTotal(lineItemsTotal);
    }

    public void applyDiscountedTotal(BigDecimal discountedTotalAmount) {

        if (discountedTotalAmount.compareTo(BigDecimal.ZERO) > 0) {
            discountedTotal = new TaxableAmount(total.getTaxRate());
            discountedTotal.calculate(discountedTotalAmount);
        }
    }

    public enum OrderState {

        /**
         * Initial state
         */
        OPEN,

        /**
         * When order is submitted.
         */
        IN_PROCESS,

        PARTIALLY_DELIVERED,

        /**
         * When order is fulfilled.
         */
        DELIVERED,

        /**
         * When order is paid.
         */
        SETTLED,

        /**
         * When order is cancelled.
         */
        CANCELLED,

        /**
         * When order is refunded.
         */
        REFUNDED,

        /**
         * When order is deleted, possibly due to testing or mistake.
         */
        DELETED,

        /**
         * When order is marked as closed to indicate it can be filtered out when trying to display current orders.
         */
        CLOSED;
    }

    public enum OrderAction {

        DELETE(OPEN, DELETED),

        /**
         * This includes scenarios of submitting the initial order, customer adding more orders during and after delivery.
         */
        SUBMIT(EnumSet.of(OPEN, IN_PROCESS, DELIVERED), IN_PROCESS),
        CANCEL(IN_PROCESS, CANCELLED),
        DELIVER(IN_PROCESS, DELIVERED),
        SETTLE(DELIVERED, SETTLED),
        REFUND(SETTLED, REFUNDED),

        /**
         * This state exists to filter out completed orders. todo: consider renaming to COMPLETED.
         */
        CLOSE(EnumSet.of(SETTLED, REFUNDED), CLOSED);

        private final EnumSet<OrderState> validFromState;

        private final OrderState validNextState;


        OrderAction(final OrderState validFromState, final OrderState validNextState) {
            this.validFromState = EnumSet.of(validFromState);
            this.validNextState = validNextState;
        }

        OrderAction(final EnumSet<OrderState> validFromState, final OrderState validNextState) {
            this.validFromState = validFromState;
            this.validNextState = validNextState;
        }

        public EnumSet<OrderState> getValidFromState() {
            return validFromState;
        }

        public OrderState getValidNextState() {
            return validNextState;
        }
    }

    @Data
    @NoArgsConstructor(access = AccessLevel.PACKAGE)
    public static class DemographicData {

        private int customerCount;

        private int male;

        private int female;

        private int kid;

        private String ageGroup;

        private String location;
    }
}
