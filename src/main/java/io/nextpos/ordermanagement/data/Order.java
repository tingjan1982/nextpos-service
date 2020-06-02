package io.nextpos.ordermanagement.data;

import io.nextpos.merchandising.data.OfferApplicableObject;
import io.nextpos.ordermanagement.service.bean.UpdateLineItem;
import io.nextpos.shared.exception.BusinessLogicException;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.model.MongoBaseObject;
import io.nextpos.shared.model.WithClientId;
import io.nextpos.tablelayout.data.TableLayout;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(Order.class);

    public static final String COPY_FROM_ORDER_ID = "copyFromOrderId";

    public static final String COPY_FROM_SERIAL_ID = "copyFromSerialId";

    public static final String ORIGINAL_ORDER_SETTINGS = "originalOrderSettings";

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
     * total or discounted total + service charge (tax inclusive)
     */
    private BigDecimal orderTotal = BigDecimal.ZERO;

    /**
     * Stores total of all the line items taking into account of line item level discount.
     */
    private TaxableAmount total;

    /**
     * Stores order level discounted total computed by order level offers.
     */
    private TaxableAmount discountedTotal;

    /**
     * if discountedTotal is not zero then, total - discountedTotal (tax inclusive)
     */
    private BigDecimal discount = BigDecimal.ZERO;

    /**
     * Service charge (tax inclusive)
     */
    private BigDecimal serviceCharge = BigDecimal.ZERO;

    private Currency currency;

    private OfferApplicableObject.AppliedOfferInfo appliedOfferInfo;

    private TableInfo tableInfo = new TableInfo();

    private String servedBy;

    private OrderSettings orderSettings;

    public DemographicData demographicData = new DemographicData();

    /**
     * Data that is not presented to the user and is not business information.
     */
    private Map<String, Object> metadata = new HashMap<>();

    private List<OrderLog> orderLogs = new ArrayList<>();

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
        this.tableInfo = new TableInfo();
        this.total = new TaxableAmount(orderSettings.getTaxRate(), orderSettings.isTaxInclusive());
        this.discountedTotal = new TaxableAmount(orderSettings.getTaxRate(), orderSettings.isTaxInclusive());
        this.currency = orderSettings.getCurrency();
        this.internalCounter = new AtomicInteger(1);
        this.orderSettings = orderSettings;
        this.demographicData = new DemographicData();

        this.addMetadata(ORIGINAL_ORDER_SETTINGS, orderSettings.copy());
    }

    public static Order newOrder(String clientId, OrderType orderType, OrderSettings orderSettings) {
        final Order order = new Order(clientId, orderSettings);
        order.setOrderType(orderType);

        return order;
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

        findMergeableLineItemByProductId(orderLineItem).ifPresentOrElse(li -> {
            LOGGER.info("Line item {} is mergeable, incrementing its quantity to {}", li, li.getQuantity() + 1);
            li.incrementQuantity();

        }, () -> {
            final String orderLineItemId = this.id + "-" + internalCounter.getAndIncrement();
            orderLineItem.setId(orderLineItemId);
            orderLineItems.add(orderLineItem);
        });
    }

    private Optional<OrderLineItem> findMergeableLineItemByProductId(OrderLineItem orderLineItem) {
        return orderLineItems.stream()
                .filter(l -> l.getProductSnapshot().getId().equals(orderLineItem.getProductSnapshot().getId()))
                .filter(li -> isLineItemMergeable(li, orderLineItem)).findFirst();
    }

    private boolean isLineItemMergeable(final OrderLineItem existingLineItem, final OrderLineItem orderLineItem) {

        if (!CollectionUtils.isEmpty(orderLineItem.getProductSnapshot().getProductOptions())) {
            return false;
        }

        if (!CollectionUtils.isEmpty(existingLineItem.getProductSnapshot().getProductOptions())) {
            return false;
        }

        if (existingLineItem.getState() != OrderLineItem.LineItemState.OPEN) {
            return false;
        }

        if (existingLineItem.getAppliedOfferInfo() != null) {
            return false;
        }

        return true;
    }

    /**
     * Quantity of 0 will remove the line item.
     *
     * @param updateLineItem
     */
    public void updateOrderLineItem(UpdateLineItem updateLineItem, Consumer<OrderLineItem> updateOperation) {

        final OrderLineItem orderLineItem = this.getOrderLineItem(updateLineItem.getLineItemId());

        if (updateLineItem.getQuantity() == 0) {
            orderLineItems.remove(orderLineItem);
        } else {
            // todo: test removing line item and verify discountTotal is reset.
            updateOperation.accept(orderLineItem);
        }

        computeTotal();
    }

    public void deleteOrderLineItem(String lineItemId) {

        final OrderLineItem orderLineItem = this.getOrderLineItem(lineItemId);
        orderLineItems.remove(orderLineItem);

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
    }

    @Override
    public void applyOffer(BigDecimal computedDiscount) {

        discountedTotal = total.newInstance();
        discountedTotal.calculate(computedDiscount);

        if (discountedTotal.lessThanZero()) {
            throw new BusinessLogicException("message.discountedTotalLessThanZero", "Discounted amount cannot be less than zero");
        }

        if (!discountedTotal.isZero()) {
            discount = total.getAmountWithTax().subtract(discountedTotal.getAmountWithTax());
        } else {
            discount = BigDecimal.ZERO;
        }

        OperationPipeline.executeDirectly(this);
    }

    public void updateServiceCharge(BigDecimal serviceCharge) {

        orderSettings.setServiceCharge(serviceCharge);

        OperationPipeline.executeDirectly(this);
    }

    public static class OperationPipeline {

        private static final Logger LOGGER = LoggerFactory.getLogger(OperationPipeline.class);

        private static ThreadLocal<OperationPipeline> pipelineInThread = new ThreadLocal<>();

        /**
         * If OperationPipeline is found in ThreadLocal, it means the reconcileOrder is to be deferred
         * and therefore is skipped in this method.
         *
         * @param order
         */
        public static void executeDirectly(Order order) {
            final OperationPipeline operationPipeline = OperationPipeline.pipelineInThread.get();

            if (operationPipeline == null) {
                new OperationPipeline().reconcileOrder(order);
            }
        }

        public static void executeAfter(Order order, Runnable runnable) {

            final OperationPipeline operationPipeline = new OperationPipeline();
            pipelineInThread.set(operationPipeline);

            try {
                runnable.run();

            } catch (Exception e) {
                throw new BusinessLogicException("Order operation pipeline failed: " + e.getMessage());
            } finally {
                operationPipeline.reconcileOrder(order);
                pipelineInThread.remove();
            }
        }

        private void reconcileOrder(Order order) {
            LOGGER.info("Reconcile order state");
            order.calculateServiceChargeAndOrderTotal();
        }
    }

    private void calculateServiceChargeAndOrderTotal() {

        final BigDecimal totalBeforeServiceCharge = discountedTotal != null && !discountedTotal.isZero() ? discountedTotal.getAmountWithTax() : total.getAmountWithTax();

        serviceCharge = orderSettings.hasServiceCharge() ? totalBeforeServiceCharge.multiply(orderSettings.getServiceCharge()) : BigDecimal.ZERO;

        orderTotal = totalBeforeServiceCharge.add(serviceCharge).setScale(orderSettings.getDecimalPlaces(), orderSettings.getRoundingMode());
    }

    public OrderDuration getOrderDuration() {
        final Duration duration = Duration.between(this.getCreatedDate().toInstant(), this.getModifiedDate().toInstant());

        return new OrderDuration(
                getCreatedDate(),
                getModifiedDate(),
                duration.toHours(),
                duration.toMinutesPart());
    }

    public void addMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    public Object getMetadata(final String key) {
        return metadata.get(key);
    }

    public void addOrderLog(OrderLog orderLog) {
        orderLogs.add(orderLog);
    }

    public Order copy() {

        final Order copy = new Order();
        copy.id = new ObjectId().toString();
        copy.lookupOrderId = copy.id;
        copy.serialId = serialId;
        copy.clientId = clientId;
        copy.orderType = orderType;
        copy.state = state;
        copy.orderTotal = orderTotal;
        copy.total = total.copy();
        copy.discountedTotal = discountedTotal != null ? discountedTotal.copy() : null;
        copy.discount = discount;
        copy.serviceCharge = serviceCharge;
        copy.currency = currency;
        copy.appliedOfferInfo = appliedOfferInfo != null ? appliedOfferInfo.copy() : null;
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
        copy.addMetadata(COPY_FROM_ORDER_ID, id);
        copy.addMetadata(COPY_FROM_SERIAL_ID, serialId);

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
        DELETE(EnumSet.of(OPEN, CANCELLED, IN_PROCESS, DELIVERED, SETTLED, REFUNDED, COMPLETED), DELETED),
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
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class TableInfo {

        private String tableLayoutId;

        private String tableLayoutName;

        private String tableId;

        private String tableName;

        private String tableNote;

        public void updateTableInfo(TableLayout.TableDetails tableDetails, String tableNote) {
            this.tableLayoutId = tableDetails.getTableLayout().getId();
            this.tableLayoutName = tableDetails.getTableLayout().getLayoutName();
            this.tableId = tableDetails.getId();
            this.tableName = tableDetails.getTableName();
            this.tableNote = tableNote;
        }

        public String getDisplayName() {
            return StringUtils.isNotBlank(tableName) ? tableName : tableNote;
        }

        public TableInfo copy() {
            return new TableInfo(tableLayoutId, tableLayoutName, tableId, tableName, tableNote);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DemographicData {

        private int male;

        private int female;

        private int kid;

        private AgeGroup ageGroup;

        private VisitFrequency visitFrequency;

        DemographicData copy() {
            return new DemographicData(male, female, kid, ageGroup, visitFrequency);
        }

        public int getCustomerCount() {
            return male + female + kid;

        }

        public enum AgeGroup {
            TWENTIES, THIRTIES, FORTIES, FIFTIES_AND_ABOVE, NOT_ENTERED
        }

        public enum VisitFrequency {
            FIRST_TIME, TWO_TO_THREE, MORE_THAN_THREE, NOT_ENTERED
        }
    }
}
