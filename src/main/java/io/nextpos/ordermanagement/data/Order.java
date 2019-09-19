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
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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

    public static final String COPY_FROM_ORDER = "copyFromOrder";

    @Id
    private String id;

    private String clientId;

    private OrderState state;

    private List<OrderLineItem> orderLineItems = new ArrayList<>();

    private TaxableAmount total;

    private TaxableAmount discountedTotal;

    private BigDecimal serviceCharge = BigDecimal.ZERO;

    private Currency currency;

    private String tableId;

    private DemographicData demographicData;

    private Map<String, Object> metadata = new HashMap<>();

    /**
     * this represents the id suffix of line item id.
     */
    private AtomicInteger internalCounter;

    @Version
    private Long version;

    public Order(final String clientId, BigDecimal taxRate, final Currency currency) {
        // todo: think of new id generation strategy
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
     * total or discounted total + service charge
     *
     * @return
     */
    public BigDecimal getOrderTotal() {

        BigDecimal serviceChargeAmount = BigDecimal.ZERO;
        final BigDecimal totalBeforeServiceCharge = discountedTotal != null ? discountedTotal.getAmountWithTax() : total.getAmountWithTax();

        if (serviceCharge.compareTo(BigDecimal.ZERO) > 0) {
            serviceChargeAmount = totalBeforeServiceCharge.multiply(serviceCharge);
        }

        return totalBeforeServiceCharge.add(serviceChargeAmount);

    }

    /**
     * Convenience method to add OrderLineItem.
     */
    public Order addOrderLineItem(ProductSnapshot productSnapshot, int quantity) {

        final OrderLineItem orderLineItem = new OrderLineItem(productSnapshot, quantity, total.getTaxRate());
        return this.addOrderLineItem(orderLineItem);
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

    public void addMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    public Object getMetadata(final String key) {
        return metadata.get(key);
    }

    public Order copy() {

        final Order copy = new Order();
        copy.id = new ObjectId().toString();
        copy.clientId = clientId;
        copy.state = state;
        copy.total = total.copy();
        copy.discountedTotal = discountedTotal != null ? discountedTotal.copy() : null;
        copy.serviceCharge = serviceCharge;
        copy.currency = currency;
        copy.tableId = tableId;
        copy.internalCounter = new AtomicInteger(1);
        copy.demographicData = demographicData != null ? demographicData.copy() : null;

        copy.orderLineItems = orderLineItems.stream()
                .map(OrderLineItem::copy)
                .peek(li -> li.setId(copy.id + "-" + copy.internalCounter.getAndIncrement()))
                .collect(Collectors.toList());

        copy.addMetadata(COPY_FROM_ORDER, id);

        return copy;
    }


    // todo: move to upper level
    public enum OrderState {

        /**
         * Initial state
         */
        OPEN,

        /**
         * When order is submitted.
         */
        IN_PROCESS,

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
         * When order is marked as completed to indicate it can be filtered out when trying to display current orders.
         */
        COMPLETED
    }

    /**
     * State machine reference:
     * https://statecharts.github.io/what-is-a-state-machine.html
     */
    public enum OrderAction {

        DELETE(OPEN, DELETED),

        /**
         * This includes scenarios of submitting the initial order, customer adding more orders during and after delivery.
         */
        SUBMIT(EnumSet.of(OPEN, IN_PROCESS, DELIVERED), IN_PROCESS),
        CANCEL(IN_PROCESS, CANCELLED),

        /**
         * Used to mark line item as delivered.
         */
        PARTIAL_DELIVER(IN_PROCESS, DELIVERED),
        DELIVER(IN_PROCESS, DELIVERED),
        SETTLE(DELIVERED, SETTLED),
        REFUND(SETTLED, REFUNDED),

        /**
         * This state exists to filter out completed orders.
         */
        COMPLETE(EnumSet.of(SETTLED, REFUNDED), COMPLETED);

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

        DemographicData copy() {
            final DemographicData demographicData = new DemographicData();
            demographicData.customerCount = customerCount;
            demographicData.male = male;
            demographicData.female =female;
            demographicData.ageGroup = ageGroup;
            demographicData.location = location;

            return demographicData;
        }
    }
}
