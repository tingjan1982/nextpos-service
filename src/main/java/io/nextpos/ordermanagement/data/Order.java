package io.nextpos.ordermanagement.data;

import io.nextpos.merchandising.data.OfferApplicableObject;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.model.MongoBaseObject;
import io.nextpos.shared.model.WithClientId;
import io.nextpos.tablelayout.data.TableLayout;
import lombok.*;
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
public class Order extends MongoBaseObject implements WithClientId, OfferApplicableObject {

    public static final String COPY_FROM_ORDER = "copyFromOrder";

    @Id
    private String id;

    /**
     * This is a human-identifiable id that can be used to call out to customers.
     */
    private String serialId;

    private String clientId;

    private OrderType orderType;

    private OrderState state;

    private List<OrderLineItem> orderLineItems = new ArrayList<>();

    /**
     * total or discounted total + service charge
     */
    private BigDecimal orderTotal;

    /**
     * Stores total of all the line items taking into account of line item level discount.
     */
    private TaxableAmount total;

    /**
     * Stores order level discounted total computed by order level offers.
     */
    private TaxableAmount discountedTotal;

    /**
     * if discountedTotal is not zero then, total - discountedTotal
     */
    private BigDecimal discount = BigDecimal.ZERO;

    private BigDecimal serviceCharge = BigDecimal.ZERO;

    private Currency currency;

    private OfferApplicableObject.AppliedOfferInfo appliedOfferInfo;

    private TableInfo tableInfo;

    private String tableNote;

    private String servedBy;

    private OrderSettings orderSettings;

    private DemographicData demographicData;

    /**
     * Data that is not presented to the user and is not business information.
     */
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * this represents the id suffix of line item id.
     */
    private AtomicInteger internalCounter;

    @Version
    private Long version;

    /**
     * Used primarily for lookup operation joining other document.
     */
    private String lookupOrderId;


    public Order(String clientId, OrderSettings orderSettings) {
        this.id = new ObjectId().toString();
        this.lookupOrderId = id;
        this.clientId = clientId;
        this.state = OPEN;
        this.total = new TaxableAmount(orderSettings.getTaxRate(), orderSettings.isTaxInclusive());
        this.currency = orderSettings.getCurrency();
        this.internalCounter = new AtomicInteger(1);
        this.orderSettings = orderSettings;
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

    public String getTableDisplayName() {
        return tableInfo != null ? tableInfo.getTableName() : tableNote;
    }

    /**
     * Convenience method to add OrderLineItem.
     */
    public void addOrderLineItem(ProductSnapshot productSnapshot, int quantity) {

        final OrderLineItem orderLineItem = new OrderLineItem(productSnapshot, quantity, orderSettings);
        this.addOrderLineItem(orderLineItem);
    }

    public void addOrderLineItems(List<OrderLineItem> orderLineItems) {

        orderLineItems.forEach(this::addLineItemToOrder);

        computeTotal();
    }

    public void addOrderLineItem(OrderLineItem orderLineItem) {

        addLineItemToOrder(orderLineItem);

        computeTotal();
    }

    private void addLineItemToOrder(final OrderLineItem orderLineItem) {

        final String orderLineItemId = this.id + "-" + internalCounter.getAndIncrement();
        orderLineItem.setId(orderLineItemId);
        orderLineItems.add(orderLineItem);
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
            // todo: test removing line item and verify discountTotal is reset.
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
                .map(OrderLineItem::getLineItemSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        total.calculate(lineItemsTotal);

        final BigDecimal discountedPrice = this.replayOfferIfExists(total);
        applyOffer(discountedPrice);

        final BigDecimal totalBeforeServiceCharge = discountedTotal != null && !discountedTotal.isZero() ? discountedTotal.getAmountWithTax() : total.getAmountWithTax();

        if (orderSettings.hasServiceCharge()) {
            serviceCharge = totalBeforeServiceCharge.multiply(orderSettings.getServiceCharge());
        }

        orderTotal = totalBeforeServiceCharge.add(serviceCharge);
    }

    @Override
    public void applyOffer(BigDecimal computedDiscount) {

        discountedTotal = total.newInstance();
        discountedTotal.calculate(computedDiscount);

        if (!discountedTotal.isZero()) {
            discount = total.getAmountWithTax().subtract(discountedTotal.getAmountWithTax());
        }

        final BigDecimal totalBeforeServiceCharge = discountedTotal.getAmountWithTax();

        if (orderSettings.hasServiceCharge()) {
            serviceCharge = totalBeforeServiceCharge.multiply(orderSettings.getServiceCharge());
        }

        orderTotal = totalBeforeServiceCharge.add(serviceCharge);
    }

    public int getCustomerCount() {

        if (demographicData != null) {
            return demographicData.male + demographicData.female + demographicData.kid;
        }

        return 0;
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
        copy.lookupOrderId = copy.id;
        copy.serialId = serialId;
        copy.clientId = clientId;
        copy.state = state;
        copy.orderTotal = orderTotal;
        copy.total = total.copy();
        copy.discountedTotal = discountedTotal != null ? discountedTotal.copy() : null;
        copy.discount = discount;
        copy.serviceCharge = serviceCharge;
        copy.currency = currency;
        copy.tableInfo = tableInfo != null ? tableInfo.copy() : null;
        copy.servedBy = servedBy;
        copy.internalCounter = new AtomicInteger(1);
        copy.demographicData = demographicData != null ? demographicData.copy() : null;

        copy.orderLineItems = orderLineItems.stream()
                .map(OrderLineItem::copy)
                .peek(li -> li.setId(copy.id + "-" + copy.internalCounter.getAndIncrement()))
                .collect(Collectors.toList());

        copy.orderSettings = orderSettings;

        copy.metadata = metadata;
        copy.addMetadata(COPY_FROM_ORDER, id);

        return copy;
    }

    public enum OrderType {
        IN_STORE, TAKE_OUT
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
        COMPLETED;

        public static List<OrderState> inflightStates() {
            return Arrays.asList(
                    Order.OrderState.OPEN,
                    Order.OrderState.IN_PROCESS,
                    Order.OrderState.DELIVERED,
                    Order.OrderState.SETTLED,
                    Order.OrderState.REFUNDED);
        }

        public static EnumSet<OrderState> finalStates() {
            return EnumSet.of(Order.OrderState.SETTLED, Order.OrderState.REFUNDED);
        }
    }

    /**
     * State machine reference:
     * https://statecharts.github.io/what-is-a-state-machine.html
     */
    public enum OrderAction {

        /**
         * This includes scenarios of submitting the initial order, customer adding more orders during and after delivery.
         */
        SUBMIT(EnumSet.of(OPEN, IN_PROCESS, DELIVERED), IN_PROCESS),
        CANCEL(EnumSet.of(OPEN, IN_PROCESS), CANCELLED),
        DELETE(EnumSet.of(OPEN, CANCELLED, IN_PROCESS, DELIVERED), DELETED),
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
    @AllArgsConstructor
    public static class TableInfo {

        private String tableLayoutId;

        private String tableLayoutName;

        private String tableId;

        private String tableName;

        public TableInfo(TableLayout.TableDetails tableDetails) {
            this.tableLayoutId = tableDetails.getTableLayout().getId();
            this.tableLayoutName = tableDetails.getTableLayout().getLayoutName();
            this.tableId = tableDetails.getId();
            this.tableName = tableDetails.getTableName();
        }

        public TableInfo copy() {
            return new TableInfo(tableLayoutId, tableLayoutName, tableId, tableName);
        }
    }

    @Data
    @NoArgsConstructor
    public static class DemographicData {

        private int male;

        private int female;

        private int kid;

        private AgeGroup ageGroup;

        private VisitFrequency visitFrequency;

        DemographicData copy() {
            final DemographicData demographicData = new DemographicData();
            demographicData.male = male;
            demographicData.female = female;
            demographicData.kid = kid;
            demographicData.ageGroup = ageGroup;
            demographicData.visitFrequency = visitFrequency;

            return demographicData;
        }

        public enum AgeGroup {
            TWENTIES, THIRTIES, FORTIES, FIFTIES_AND_ABOVE
        }

        public enum VisitFrequency {
            FIRST_TIME, TWO_TO_THREE, MORE_THAN_THREE
        }
    }
}
