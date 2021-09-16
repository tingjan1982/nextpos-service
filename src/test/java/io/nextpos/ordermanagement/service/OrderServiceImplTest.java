package io.nextpos.ordermanagement.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientRepository;
import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.datetime.service.ZonedDateRangeBuilder;
import io.nextpos.merchandising.data.ProductLevelOffer;
import io.nextpos.ordermanagement.data.*;
import io.nextpos.ordermanagement.service.bean.LineItemOrdering;
import io.nextpos.ordermanagement.service.bean.UpdateLineItem;
import io.nextpos.reporting.data.DateParameterType;
import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.exception.BusinessLogicException;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import io.nextpos.tablelayout.data.TableLayout;
import io.nextpos.tablelayout.service.TableLayoutService;
import io.nextpos.workingarea.data.WorkingArea;
import io.nextpos.workingarea.service.WorkingAreaService;
import org.assertj.core.data.Index;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ChainedTransaction
class OrderServiceImplTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderServiceImplTest.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private ShiftService shiftService;

    @Autowired
    private OrderSettings orderSettings;

    @Autowired
    private TableLayoutService tableLayoutService;

    @Autowired
    private WorkingAreaService workingAreaService;

    @Autowired
    private OrderIdCounterRepository orderIdCounterRepository;

    @Autowired
    private Client client;

    @Autowired
    private ClientRepository clientRepository;

    private TableLayout.TableDetails table1;

    private TableLayout.TableDetails table2;


    @BeforeEach
    void prepare() {
        final TableLayout tableLayout = DummyObjects.dummyTableLayout(client);
        tableLayoutService.saveTableLayout(tableLayout);

        table1 = tableLayout.getTables().get(0);
        table2 = tableLayout.getTables().get(1);
    }

    @Test
    @WithMockUser("dummyUser")
    void crudOrder() {

        final Order order = Order.newOrder(client.getId(), Order.OrderType.IN_STORE, orderSettings);
        order.updateTables(List.of(table1));

        final List<ProductSnapshot.ProductOptionSnapshot> options = List.of(
                new ProductSnapshot.ProductOptionSnapshot("ice", "1/3"),
                new ProductSnapshot.ProductOptionSnapshot("sugar", "none", BigDecimal.valueOf(10))
        );

        final ProductSnapshot product = new ProductSnapshot(UUID.randomUUID().toString(), "coffee", "tw01", BigDecimal.valueOf(100), options);
        order.addOrderLineItem(product, 1);
        order.addOrderLineItem(product, 2);

        final Order createdOrder = orderService.createOrder(order);

        assertThat(createdOrder.getId()).isNotNull();
        assertThat(createdOrder.getClientId()).isEqualTo(client.getId());
        assertThat(createdOrder.getState()).isEqualTo(Order.OrderState.OPEN);
        assertThat(createdOrder.getTotal()).satisfies(total -> {
            assertThat(total.getAmountWithoutTax()).isEqualByComparingTo("330");
            assertThat(total.getAmountWithTax()).isEqualByComparingTo("346.5");
            assertThat(total.getTax()).isEqualByComparingTo("16.5");
        });
        assertThat(createdOrder.getOrderTotal()).isEqualByComparingTo("381.15");

        assertThat(createdOrder.getOrderLineItems()).hasSize(2);
        assertThat(createdOrder.getOrderLineItems()).satisfies(li -> {
            assertThat(li.getState()).isEqualTo(OrderLineItem.LineItemState.OPEN);
            assertThat(li.getQuantity()).isEqualTo(2);
            assertThat(li.getProductSnapshot()).satisfies(p -> {
                assertThat(p.getName()).isEqualTo(product.getName());
                assertThat(p.getSku()).isEqualTo(product.getSku());
                assertThat(p.getPrice()).isEqualTo(product.getPrice());
            });
            assertThat(li.getSubTotal()).satisfies(subTotal -> {
                assertThat(subTotal.getAmountWithoutTax()).isEqualByComparingTo(new BigDecimal("220"));
                assertThat(subTotal.getAmountWithTax()).isEqualByComparingTo(new BigDecimal("231"));
                assertThat(subTotal.getTax()).isEqualByComparingTo(new BigDecimal("11"));
            });

        }, Index.atIndex(1));

        final Order existingOrder = orderService.getOrder(createdOrder.getId());

        assertThat(existingOrder).isEqualToIgnoringGivenFields(createdOrder, "internalCounter");

        final List<Order> inflightOrders = orderService.getInflightOrders(client.getId());

        assertThat(inflightOrders).hasSize(1);

        orderService.markOrderAsDeleted(existingOrder.getId(), true);

        final Order deletedOrder = orderService.getOrder(existingOrder.getId());

        assertThat(deletedOrder.getOrderLineItems()).isEmpty();
        assertThat(deletedOrder.getDeletedOrderLineItems()).isNotEmpty();

        assertThat(orderService.getInflightOrders(client.getId())).isEmpty();
    }

    @Test
    @WithMockUser("dummyUser")
    void addAndUpdateOrderLineItem() {

        final Order order = new Order(client.getId(), orderSettings);
        final OrderLineItem orderLineItem = new OrderLineItem(DummyObjects.productSnapshot(), 1, orderSettings);
        order.addOrderLineItem(orderLineItem);
        orderService.createOrder(order);
        orderService.deleteOrderLineItem(order, orderLineItem.getId());

        Shift shift = shiftService.getActiveShiftOrThrows(client.getId());
        assertThat(shift.getDeletedLineItems()).isEmpty();

        final Order orderWithLineItem = orderService.addOrderLineItem(client, order, orderLineItem);
        
        assertThat(orderWithLineItem.getOrderLineItems()).hasSize(1);

        final OrderLineItem productSetLineItem = new OrderLineItem(DummyObjects.productSnapshot("combo", new BigDecimal("100")), 2, orderSettings);
        final ProductSnapshot rice = DummyObjects.productSnapshot("rice", new BigDecimal("100"));
        rice.setOverridePrice(BigDecimal.ZERO);
        productSetLineItem.getChildLineItems().add(new OrderLineItem(rice, 2, orderSettings));
        final ProductSnapshot drink = DummyObjects.productSnapshot("drink", new BigDecimal("40"));
        drink.setOverridePrice(BigDecimal.ZERO);
        productSetLineItem.getChildLineItems().add(new OrderLineItem(drink, 2, orderSettings));

        orderService.addOrderLineItem(client, orderWithLineItem, productSetLineItem);

        final OrderLineItem anotherProductSet = productSetLineItem.copy();
        orderService.addOrderLineItem(client, orderWithLineItem, anotherProductSet);

        final ProductSnapshot anotherRice = rice.copy();
        anotherRice.setOverridePrice(null);

        orderService.addOrderLineItem(client, orderWithLineItem, new OrderLineItem(anotherRice, 1, orderSettings));

        assertThat(orderWithLineItem.getOrderLineItems()).hasSize(5);

        final OrderStateChangeBean stateChange = orderService.performOrderAction(order.getId(), Order.OrderAction.SUBMIT);
        final Order submittedOrder = stateChange.getOrder();

        assertThatThrownBy(() -> orderService.deleteOrderLineItem(submittedOrder, productSetLineItem.getChildLineItems().get(0).getId()),
                "cannot perform on product set line item").isInstanceOf(BusinessLogicException.class);

        orderService.deleteOrderLineItem(submittedOrder, productSetLineItem.getId());

        assertThat(submittedOrder.getOrderLineItems()).hasSize(2);
        assertThat(submittedOrder.getDeletedOrderLineItems()).hasSize(4);

        Shift activeShift = shiftService.getActiveShiftOrThrows(client.getId());
        assertThat(activeShift.getDeletedLineItems()).hasSize(1);

        final List<ProductSnapshot.ProductOptionSnapshot> productOptions = List.of(DummyObjects.productOptionSnapshot());
        UpdateLineItem updateLineItem = new UpdateLineItem(orderLineItem.getId(), 5, null, null, productOptions, ProductLevelOffer.GlobalProductDiscount.DISCOUNT_AMOUNT_OFF, new BigDecimal(20));

        Order updatedOrder = orderService.updateOrderLineItem(submittedOrder, updateLineItem);

        assertThat(updatedOrder.getOrderLineItems()).satisfies(li -> {
            assertThat(li.getQuantity()).isEqualTo(5);
            assertThat(li.getProductPriceWithOptions().getAmountWithoutTax()).isEqualByComparingTo("110");
            assertThat(li.getProductPriceWithOptions().getAmountWithTax()).isEqualByComparingTo("115.5");
            assertThat(li.getProductPriceWithOptions().getAmount()).isEqualByComparingTo("110");
            assertThat(li.getSubTotal().getAmountWithoutTax()).isEqualByComparingTo("550");
            assertThat(li.getSubTotal().getAmountWithTax()).isEqualByComparingTo("577.5");
            assertThat(li.getSubTotal().getAmount()).isEqualByComparingTo("550");
            assertThat(li.getDiscountedSubTotal().getAmountWithoutTax()).isEqualByComparingTo("450");
            assertThat(li.getDiscountedSubTotal().getAmountWithTax()).isEqualByComparingTo("472.5");
            assertThat(li.getDiscountedSubTotal().getAmount()).isEqualByComparingTo("450");
            assertThat(li.getLineItemSubTotal()).isEqualByComparingTo("450");
        }, Index.atIndex(0));

        assertThat(updatedOrder).satisfies(o -> {
            assertThat(o.getDiscount()).isEqualByComparingTo("0");
            assertThat(o.getDiscountedTotal().getAmountWithoutTax()).isEqualByComparingTo("0");
            assertThat(o.getDiscountedTotal().getAmountWithTax()).isEqualByComparingTo("0");
            assertThat(o.getDiscountedTotal().getAmount()).isEqualByComparingTo("0");
            assertThat(o.getServiceCharge()).isEqualByComparingTo("57.75");
            assertThat(o.getOrderTotal()).isEqualByComparingTo(BigDecimal.valueOf(((110 - 20) * 5 + 100) * 1.05 * 1.1));
        });

        updateLineItem = new UpdateLineItem(orderLineItem.getId(), 5, null, new BigDecimal("50"), productOptions, ProductLevelOffer.GlobalProductDiscount.NO_DISCOUNT, BigDecimal.ZERO);
        updatedOrder = orderService.updateOrderLineItem(updatedOrder, updateLineItem);

        assertThat(updatedOrder.getOrderLineItems()).satisfies(li -> {
            assertThat(li.getProductSnapshot().getProductPriceWithOptions()).isEqualByComparingTo("50");
            assertThat(li.getProductPriceWithOptions().getAmountWithoutTax()).isEqualByComparingTo("50");
            assertThat(li.getProductPriceWithOptions().getAmountWithTax()).isEqualByComparingTo("52.5");
            assertThat(li.getProductPriceWithOptions().getAmount()).isEqualByComparingTo("50");
            assertThat(li.getSubTotal().getAmountWithoutTax()).isEqualByComparingTo("250");
            assertThat(li.getSubTotal().getAmountWithTax()).isEqualByComparingTo("262.5");
            assertThat(li.getSubTotal().getAmount()).isEqualByComparingTo("250");
            assertThat(li.getDiscountedSubTotal().getAmountWithoutTax()).isEqualByComparingTo("0");
            assertThat(li.getDiscountedSubTotal().getAmountWithTax()).isEqualByComparingTo("0");
            assertThat(li.getDiscountedSubTotal().getAmount()).isEqualByComparingTo("0");
            assertThat(li.getLineItemSubTotal()).isEqualByComparingTo("250");
        }, Index.atIndex(0));

        orderService.updateOrderLineItemPrice(updatedOrder, orderLineItem.getId(), BigDecimal.ZERO);

        activeShift = shiftService.getActiveShiftOrThrows(client.getId());
        assertThat(activeShift.getDeletedLineItems()).hasSize(2);

        assertThat(updatedOrder.getOrderLineItem(orderLineItem.getId()).getLineItemSubTotal()).isEqualByComparingTo("0");
    }

    @Test
    void moveOrder() {

        Order sourceOrder = Order.newOrder(client.getId(), Order.OrderType.IN_STORE, orderSettings);
        sourceOrder.getDemographicData().setMale(1);
        sourceOrder.getDemographicData().setFemale(1);
        sourceOrder.getDemographicData().setKid(1);

        sourceOrder.updateTables(List.of(table1));
        sourceOrder.addOrderLineItem(DummyObjects.productSnapshot("coffee", new BigDecimal("45")), 1);
        orderService.createOrder(sourceOrder);
        orderService.performOrderAction(sourceOrder.getId(), Order.OrderAction.SUBMIT);

        Order sourceOrder2 = orderService.getOrder(sourceOrder.getId());
        sourceOrder2.addOrderLineItem(DummyObjects.productSnapshot("coffee", new BigDecimal("45")), 1);
        orderService.saveOrder(sourceOrder2);

        assertThat(sourceOrder2.getOrderLineItems()).hasSize(2);
        assertThat(sourceOrder2.getState()).isEqualByComparingTo(Order.OrderState.IN_PROCESS);

        final Order targetOrder = Order.newOrder(client.getId(), Order.OrderType.IN_STORE, orderSettings);
        targetOrder.getDemographicData().setMale(1);
        targetOrder.getDemographicData().setFemale(1);
        targetOrder.getDemographicData().setKid(1);
        targetOrder.updateTables(List.of(table2));
        targetOrder.addOrderLineItem(DummyObjects.productSnapshot("coffee", new BigDecimal("45")), 1);
        targetOrder.addOrderLineItem(DummyObjects.productSnapshot("tea", new BigDecimal("30")), 1);
        orderService.saveOrder(targetOrder);

        assertThat(targetOrder.getOrderLineItems()).hasSize(2);

        final Order updatedOrder = orderService.moveOrder(sourceOrder.getId(), targetOrder.getId());
        assertThat(updatedOrder.getOrderLineItems()).hasSize(3);
        assertThat(updatedOrder.getId()).isEqualTo(targetOrder.getId());
        assertThat(updatedOrder.getDemographicData()).satisfies(d -> {
            assertThat(d.getMale()).isEqualTo(2);
            assertThat(d.getFemale()).isEqualTo(2);
            assertThat(d.getKid()).isEqualTo(2);
        });
        assertThat(orderService.getOrder(sourceOrder.getId()).getState()).isEqualByComparingTo(Order.OrderState.DELETED);

        final Shift activeShift = shiftService.getActiveShiftOrThrows(client.getId());
        assertThat(activeShift.getDeletedLineItems()).isEmpty();
    }

    @Test
    void getOrders() {

        final LocalDateTime fromDate = LocalDateTime.now();
        final LocalDateTime toDate = fromDate.plusDays(1);
        LOGGER.info("Date range: {}, {}", fromDate, toDate);

        final Order order = new Order(client.getId(), orderSettings);
        order.updateTables(List.of(table1));
        orderService.createOrder(order);

        final ZonedDateRange zonedDateRange = ZonedDateRangeBuilder.builder(client, DateParameterType.RANGE).dateRange(fromDate, toDate).build();

        final List<Order> orders = orderService.getOrders(client, zonedDateRange);

        LOGGER.info("Orders: {}", orders);
        assertThat(orders).isNotEmpty();

        assertThat(orderService.getOrders(client, zonedDateRange, OrderCriteria.instance().tableName("dummy-table1"))).isNotEmpty();
        assertThat(orderService.getOrders(client, zonedDateRange, OrderCriteria.instance().orderState(Order.OrderState.OPEN))).isNotEmpty();
        assertThat(orderService.getOrders(client, zonedDateRange, OrderCriteria.instance().orderState(Order.OrderState.IN_PROCESS))).isEmpty();

        final ZonedDateRange zonedDateRange2 = ZonedDateRangeBuilder.builder(client, DateParameterType.RANGE).dateRange(toDate, toDate.plusDays(1)).build();

        final List<Order> shouldBeEmpty = orderService.getOrders(client, zonedDateRange2);
        assertThat(shouldBeEmpty).isEmpty();
    }

    @Test
    void inProcessOrderLineItems() {

        shiftService.openShift(client.getId(), BigDecimal.ZERO);
        final InProcessOrderLineItems emptyOrderLineItems = orderService.getInProcessOrderLineItems(client.getId());

        assertThat(emptyOrderLineItems.getResults()).isEmpty();

        final WorkingArea bar = new WorkingArea(client, "bar");
        workingAreaService.saveWorkingArea(bar);

        final Order order1 = Order.newOrder(client.getId(), Order.OrderType.IN_STORE, orderSettings);
        order1.addOrderLineItem(DummyObjects.productSnapshot(), 1);
        final OrderLineItem orderLineItem = new OrderLineItem(DummyObjects.productSnapshot(), 2, orderSettings);
        orderLineItem.setWorkingAreaId(bar.getId());
        order1.addOrderLineItem(orderLineItem);
        orderService.saveOrder(order1);
        orderService.performOrderAction(order1.getId(), Order.OrderAction.SUBMIT);

        final Order order2 = Order.newOrder(client.getId(), Order.OrderType.IN_STORE, orderSettings);
        OrderLineItem li2 = order2.addOrderLineItem(DummyObjects.productSnapshot(), 3);
        OrderLineItem li3 = order2.addOrderLineItem(DummyObjects.productSnapshot(), 4);
        OrderLineItem li4 = order2.addOrderLineItem(DummyObjects.productSnapshot(), 5);
        orderService.saveOrder(order2);
        orderService.performOrderAction(order2.getId(), Order.OrderAction.SUBMIT);

        final InProcessOrderLineItems orderLineItems = orderService.getInProcessOrderLineItems(client.getId());

        assertThat(orderLineItems.getResults()).hasSize(2);

        orderService.orderLineItems(List.of(createLineItemOrdering(order2, li4),
                createLineItemOrdering(order2, li3),
                createLineItemOrdering(order2, li2)));

        final InProcessOrderLineItems sortedOrderLineItems = orderService.getInProcessOrderLineItems(client.getId());

        assertThat(sortedOrderLineItems.getResults()).hasSize(2);
        assertThat(sortedOrderLineItems.getResults().values()).allSatisfy(lis -> {
            assertThat(lis).isSortedAccordingTo(InProcessOrderLineItem.getComparator());
        });

        orderService.markAllLineItemsAsPrepared(client.getId());

        assertThat(orderService.getInProcessOrderLineItems(client.getId()).getResults()).isEmpty();
    }

    private LineItemOrdering createLineItemOrdering(Order order, OrderLineItem orderLineItem) {
        return new LineItemOrdering(order.getId(), orderLineItem.getId());
    }

    @Test
    void copyOrder() {

        final Order order = Order.newOrder(client.getId(), Order.OrderType.IN_STORE, orderSettings);

        order.addOrderLineItem(DummyObjects.productSnapshot(), 5);

        final ProductSnapshot productWithOption = new ProductSnapshot("pid",
                "custom",
                null,
                BigDecimal.valueOf(200),
                List.of(new ProductSnapshot.ProductOptionSnapshot("ice", "normal", BigDecimal.valueOf(5))));
        final OrderLineItem lineItem = new OrderLineItem(productWithOption, 2, orderSettings);
        order.addOrderLineItem(lineItem);

        orderService.createOrder(order);

        final Order copiedOrder = orderService.copyOrder(order.getId());

        assertThat(copiedOrder.getMetadata(Order.COPY_FROM_ORDER_ID)).isEqualTo(order.getId());
        assertThat(copiedOrder.getMetadata(Order.COPY_FROM_SERIAL_ID)).isEqualTo(order.getSerialId());
        assertThat(copiedOrder).isEqualToIgnoringGivenFields(order, "id", "serialId", "lookupOrderId", "orderLineItems", "metadata", "internalCounter", "createdDate", "modifiedDate");
        assertThat(copiedOrder.getOrderLineItems()).usingElementComparatorIgnoringFields("id", "createdDate", "modifiedDate").isEqualTo(order.getOrderLineItems());
    }

    @Test
    void checkOrderTotalRounding() {

        final Order order = new Order(client.getId(), orderSettings);
        order.addOrderLineItem(DummyObjects.productSnapshot("coffee", new BigDecimal("9.5")), 1);

        assertThat(order.getOrderTotal()).isEqualTo("10.97"); // from 10.9725

        final OrderSettings copiedOrderSettings = orderSettings.copy();
        copiedOrderSettings.setDecimalPlaces(0);
        copiedOrderSettings.setRoundingMode(null);

        order.setOrderSettings(copiedOrderSettings);
        order.computeTotal();

        assertThat(order.getOrderTotal()).isEqualTo("11");
    }

    @Test
    void generateSerialId() {

        final String todayIdPrefix = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

        assertThat(orderService.generateSerialId(client.getId())).isEqualTo("%s-%s", todayIdPrefix, 1);
        assertThat(orderService.generateSerialId(client.getId())).isEqualTo("%s-%s", todayIdPrefix, 2);
        assertThat(orderService.generateSerialId(client.getId())).isEqualTo("%s-%s", todayIdPrefix, 3);
        assertThat(orderService.generateSerialId(client.getId())).isEqualTo("%s-%s", todayIdPrefix, 4);
    }
}