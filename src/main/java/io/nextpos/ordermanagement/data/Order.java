package io.nextpos.ordermanagement.data;

import io.nextpos.membership.data.Membership;
import io.nextpos.merchandising.data.OfferApplicableObject;
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
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
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
public class Order extends MongoBaseObject implements WithClientId, OfferApplicableObject, OrderLineItemOperation {

    private static final Logger LOGGER = LoggerFactory.getLogger(Order.class);

    public static final String COPY_FROM_ORDER_ID = "copyFromOrderId";

    public static final String COPY_FROM_SERIAL_ID = "copyFromSerialId";

    public static final String ORIGINAL_ORDER_SETTINGS = "originalOrderSettings";

    public static final String SOURCE_ORDER_ID = "originalSplitOrder";

    public static final String ORDER_SET_ORDER = "orderSetOrder";

    public static final String HEAD_COUNT = "headCount";

    public static final String PREVIOUS_TABLES = "previousTables";

    @Id
    private String id;

    /**
     * This is a human-identifiable id that can be used to call out to customers.
     */
    private String serialId;

    @Indexed
    private String clientId;

    private OrderType orderType;

    private OrderState state;

    private List<OrderLineItem> orderLineItems = new ArrayList<>();

    private List<OrderLineItem> deletedOrderLineItems = new ArrayList<>();

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
     * Zero indicates no discount unless fullDiscount is set to true, which means the order total is zero.
     */
    private TaxableAmount discountedTotal;

    /**
     * This has an effect on the value of discountedTotal when set to true.
     * See calculateServiceChargeAndOrderTotal() for usage reference.
     */
    private boolean fullDiscount;

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

    private List<TableInfo> tables = new ArrayList<>();

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

    @DBRef
    @Indexed
    private Membership membership;

    @Indexed
    private String lookupMembershipId;

    /**
     * The position which to display this order on screen.
     */
    private int order;

    public Order(String clientId, OrderSettings orderSettings) {
        this.id = new ObjectId().toString();
        this.lookupOrderId = id;
        this.clientId = clientId;
        this.state = OPEN;
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

    public boolean isPaying() {
        return this.state == PAYMENT_IN_PROCESS;
    }

    public boolean isClosed() {
        return OrderState.finalStates().contains(this.state);
    }

    public boolean isTablesEmpty() {
        return tables.stream().allMatch(TableInfo::isEmpty);
    }

    public void updateMembership(Membership membership) {
        OrderVisitors.accept(this, OrderVisitors.UpdateMembership.instance(membership));
    }

    public void updateTables(List<TableLayout.TableDetails> tableDetails) {
        OrderVisitors.accept(this, OrderVisitors.UpdateTables.instance(tableDetails));
    }

    public void addTableNote(String tableNote) {
        final TableInfo tableInfo = new TableInfo();
        tableInfo.setTableNote(tableNote);

        tables.add(tableInfo);
    }

    public String getTableNames() {
        return tables.stream()
                .map(TableInfo::getDisplayName)
                .collect(Collectors.joining(", "));
    }

    public TableInfo getOneTableInfo() {
        return tables.isEmpty() ? new TableInfo() : tables.get(0);
    }

    public void mergeDemographicData(DemographicData demographicDataToMerge) {
        this.demographicData.male = this.demographicData.male + demographicDataToMerge.male;
        this.demographicData.female = this.demographicData.female + demographicDataToMerge.female;
        this.demographicData.kid = this.demographicData.kid + demographicDataToMerge.kid;
    }

    /**
     * Convenience method to add OrderLineItem.
     */
    public OrderLineItem addOrderLineItem(ProductSnapshot productSnapshot, int quantity) {

        final OrderLineItem orderLineItem = new OrderLineItem(productSnapshot, quantity, orderSettings);
        this.addOrderLineItem(orderLineItem);

        return orderLineItem;
    }

    public void addOrderLineItems(List<OrderLineItem> orderLineItems) {

        orderLineItems.forEach(this::addLineItemToOrder);

        computeTotal();
    }

    @Override
    public void addOrderLineItem(OrderLineItem orderLineItem) {

        addLineItemToOrder(orderLineItem);

        computeTotal();
    }

    private void addLineItemToOrder(final OrderLineItem orderLineItem) {

        findMergeableLineItemByProductId(orderLineItem).ifPresentOrElse(li -> {
            LOGGER.info("Line item {} is mergeable, updating its quantity to {}", li, li.getQuantity() + orderLineItem.getQuantity());
            li.incrementQuantity(orderLineItem.getQuantity());

            // line item that is mergeable will inherit id from the found line item id to make product set scenario work.
            orderLineItem.setId(li.getId());

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

        final boolean existingLiEligible = checkLineItemState(existingLineItem);
        final boolean newLiEligible = checkLineItemState(orderLineItem);

        if (!existingLiEligible || !newLiEligible) {
            return false;
        }

        if (!StringUtils.equals(orderLineItem.getAssociatedLineItemId(), existingLineItem.getAssociatedLineItemId())) {
            return false;
        }

        final String newSku = orderLineItem.getProductSnapshot().getSku();
        final String existingSku = existingLineItem.getProductSnapshot().getSku();

        if (!StringUtils.equals(newSku, existingSku)) {
            return false;
        }

        if (orderLineItem.getComboTotal().compareTo(BigDecimal.ZERO) > 0) {
            return false;
        }

        return true;
    }

    private boolean checkLineItemState(OrderLineItem lineItem) {

        if (!CollectionUtils.isEmpty(lineItem.getProductSnapshot().getProductOptions())) {
            return false;
        }

        if (lineItem.getState() != OrderLineItem.LineItemState.OPEN) {
            return false;
        }

        if (lineItem.getAppliedOfferInfo() != null) {
            return false;
        }

        if (lineItem.getProductSnapshot().getOverridePrice() != null && StringUtils.isBlank(lineItem.getAssociatedLineItemId())) {
            return false;
        }

        return true;
    }

    @Override
    public void addSplitOrderLineItem(OrderLineItem sourceOrderLineItem, Order sourceOrder) {

        this.orderLineItems.add(sourceOrderLineItem.splitCopy());
        sourceOrder.updateOrderLineItem(sourceOrderLineItem, li -> li.decrementQuantity(1));

        computeTotal();
    }

    @Override
    public OrderLineItem updateOrderLineItem(String lineItemId, Consumer<OrderLineItem> updateOperation) {

        final OrderLineItem orderLineItem = this.getOrderLineItem(lineItemId);
        this.updateOrderLineItem(orderLineItem, updateOperation);

        return orderLineItem;
    }

    @Override
    public void updateOrderLineItem(OrderLineItem orderLineItem, Consumer<OrderLineItem> updateOperation) {

        orderLineItem.performOperation(updateOperation);

        if (orderLineItem.getQuantity() == 0) {
            orderLineItems.remove(orderLineItem);
        }

        computeTotal();
    }

    @Override
    public void deleteOrderLineItem(OrderLineItem orderLineItem) {

        deletedOrderLineItems.add(orderLineItem);
        orderLineItems.remove(orderLineItem);

        computeTotal();
    }

    public void deleteAllOrderLineItems() {

        deletedOrderLineItems.addAll(orderLineItems);
        orderLineItems.clear();

        //computeTotal();
    }

    public OrderLineItem getOrderLineItem(String lineItemId) {
        return orderLineItems.stream()
                .filter(li -> StringUtils.equals(li.getId(), lineItemId))
                .findFirst().orElseThrow(() -> {
                    throw new ObjectNotFoundException(lineItemId, OrderLineItem.class);
                });
    }

    public Optional<OrderLineItem> findOrderLineItem(String lineItemId) {
        return orderLineItems.stream()
                .filter(li -> StringUtils.equals(li.getId(), lineItemId))
                .findFirst();
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
            this.throwDiscountLessThanZeroException();
        }

        if (discountedTotal.isZero() && !fullDiscount) {
            discount = BigDecimal.ZERO;
        } else {
            discount = total.getAmountWithTax().subtract(discountedTotal.getAmountWithTax());
        }

        OperationPipeline.executeDirectly(this);
    }

    public BigDecimal getOrderTotalWithoutServiceCharge() {
        return deduceRoundingAmount(() -> orderTotal.subtract(serviceCharge));
    }

    public BigDecimal getOrderTotalWithoutTax() {
        final TaxableAmount taxableAmount = !discountedTotal.isZero() || fullDiscount ? discountedTotal : total;

        return this.deduceRoundingAmount(taxableAmount::getAmountWithoutTax);
    }

    public BigDecimal getOrderTotalTax() {
        final TaxableAmount taxableAmount = !discountedTotal.isZero() || fullDiscount ? discountedTotal : total;

        return this.deduceRoundingAmount(taxableAmount::getTax);
    }

    public void updateServiceCharge(BigDecimal serviceCharge) {

        orderSettings.setServiceCharge(serviceCharge);

        OperationPipeline.executeDirectly(this);
    }

    public BigDecimal deduceRoundingAmount(Supplier<BigDecimal> amount) {
        return amount.get().setScale(orderSettings.getDecimalPlaces(), orderSettings.getRoundingMode());
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

        BigDecimal deducedTotal = !discountedTotal.isZero() || fullDiscount ? discountedTotal.getAmountWithTax() : total.getAmountWithTax();

        serviceCharge = BigDecimal.ZERO;

        if (orderSettings.hasServiceCharge() && deducedTotal.compareTo(BigDecimal.ZERO) > 0) {
            serviceCharge = total.getAmountWithTax().multiply(orderSettings.getServiceCharge());
            serviceCharge = this.deduceRoundingAmount(() -> serviceCharge);
        }

        orderTotal = this.deduceRoundingAmount(() -> deducedTotal.add(serviceCharge));
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

    public void removeMetadata(String key) {
        metadata.remove(key);
    }

    public Object getMetadata(final String key) {
        return metadata.get(key);
    }

    public void addOrderLog(OrderLog orderLog) {
        orderLogs.add(orderLog);
    }

    public void markSplitOrder(Order sourceOrder) {
        this.addTableNote("SplitOrder");
        this.addMetadata(SOURCE_ORDER_ID, sourceOrder.getId());
    }

    public boolean isSplitOrder() {
        return getMetadata(SOURCE_ORDER_ID) != null;
    }

    public String getSourceOrderId() {
        return (String) getMetadata(SOURCE_ORDER_ID);
    }

    public void markOrderSetOrder() {
        this.addMetadata(ORDER_SET_ORDER, Boolean.TRUE);
    }

    public boolean isOrderSetOrder() {
        return getMetadata(ORDER_SET_ORDER) != null;
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
        copy.tables = tables.stream()
                .map(TableInfo::copy)
                .collect(Collectors.toList());
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

    public ProductSetOrder productSetOrder() {
        return ProductSetOrder.newInstance(this);
    }

    public enum OrderType {
        IN_STORE, TAKE_OUT, ONLINE
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

        PAYMENT_IN_PROCESS,

        /**
         * When order is paid.
         */
        SETTLED,

        /**
         * When order is voided/cancelled.
         */
        CANCELLED,

        /**
         * When order is refunded.
         */
        REFUNDED,

        VOIDED,

        /**
         * When order is deleted, possibly due to testing or mistake.
         */
        DELETED,

        /**
         * When order is marked as completed to indicate it can be filtered out when trying to display current orders.
         */
        COMPLETED,

        /**
         * State remains unchanged.
         */
        RETAIN_STATE,

        /**
         * Go back to the previous state.
         */
        PREV_FROM_STATE;

        public static List<OrderState> inflightStates() {
            return Arrays.asList(
                    OPEN,
                    IN_PROCESS,
                    DELIVERED,
                    PAYMENT_IN_PROCESS,
                    SETTLED);
        }

        public static EnumSet<OrderState> finalStates() {
            return EnumSet.of(SETTLED, COMPLETED, REFUNDED, CANCELLED, VOIDED);
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
        DELETE(EnumSet.of(OPEN, IN_PROCESS, DELIVERED, PAYMENT_IN_PROCESS, SETTLED, REFUNDED, COMPLETED), DELETED),

        /**
         * Used in realtime order to indicate the order is prepared and ready to be served.
         */
        PREPARE(EnumSet.of(IN_PROCESS, SETTLED, COMPLETED), RETAIN_STATE),
        /**
         * Used to mark line item as delivered.
         */
        PARTIAL_DELIVER(IN_PROCESS, DELIVERED),
        DELIVER(IN_PROCESS, DELIVERED),
        POST_DELIVER(SETTLED, RETAIN_STATE),
        ENTER_PAYMENT(EnumSet.of(IN_PROCESS, DELIVERED), PAYMENT_IN_PROCESS),
        EXIT_PAYMENT(PAYMENT_IN_PROCESS, PREV_FROM_STATE),
        SETTLE(EnumSet.of(IN_PROCESS, DELIVERED, PAYMENT_IN_PROCESS), SETTLED),
        CANCEL(EnumSet.of(SETTLED, COMPLETED, DELETED), CANCELLED),
        VOID(EnumSet.of(SETTLED, COMPLETED, CANCELLED, DELETED), VOIDED),
        REFUND(SETTLED, REFUNDED),

        /**
         * This state exists to filter out completed orders.
         */
        COMPLETE(EnumSet.of(SETTLED), COMPLETED);

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
    @NoArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class TableInfo {

        public static final String NO_LAYOUT = "NO_LAYOUT";

        private String tableLayoutId = NO_LAYOUT;

        private String tableLayoutName = "No Layout";

        private String tableId;

        private String tableName;

        private String tableNote;

        public TableInfo(TableLayout.TableDetails tableDetails) {
            this.updateTableInfo(tableDetails, null);
        }

        public void updateTableInfo(TableLayout.TableDetails tableDetails, String tableNote) {
            this.tableLayoutId = tableDetails.getTableLayout().getId();
            this.tableLayoutName = tableDetails.getTableLayout().getLayoutName();
            this.tableId = tableDetails.getId();
            this.tableName = tableDetails.getTableName();
            this.tableNote = tableNote;
        }

        public boolean isEmpty() {
            return StringUtils.isEmpty(tableId);
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
