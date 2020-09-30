package io.nextpos.ordermanagement.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.datetime.service.ZonedDateRangeBuilder;
import io.nextpos.merchandising.data.ProductLevelOffer;
import io.nextpos.ordermanagement.data.*;
import io.nextpos.ordermanagement.service.bean.UpdateLineItem;
import io.nextpos.reporting.data.DateParameterType;
import io.nextpos.shared.DummyObjects;
import io.nextpos.tablelayout.data.TableLayout;
import io.nextpos.tablelayout.service.TableLayoutService;
import org.assertj.core.data.Index;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class OrderServiceImplTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderServiceImplTest.class);

    @Autowired
    private ClientService clientService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderSettings orderSettings;

    @Autowired
    private TableLayoutService tableLayoutService;

    @Autowired
    private OrderIdCounterRepository orderIdCounterRepository;

    private Client client;

    private TableLayout.TableDetails tableDetails;


    @BeforeEach
    void prepare() {
        client = DummyObjects.dummyClient();
        clientService.saveClient(client);

        final TableLayout tableLayout = DummyObjects.dummyTableLayout(this.client);
        tableLayoutService.saveTableLayout(tableLayout);

        tableDetails = tableLayout.getTables().get(0);
    }

    @Test
    @WithMockUser("dummyUser")
    void createAndGetOrder() {

        final Order order = new Order(client.getId(), orderSettings);
        order.getTableInfo().updateTableInfo(tableDetails, null);

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
    }

    @Test
    @WithMockUser("dummyUser")
    void addAndUpdateOrderLineItem() {

        final Order order = new Order(client.getId(), orderSettings);
        final Order createdOrder = orderService.createOrder(order);
        final OrderLineItem orderLineItem = new OrderLineItem(DummyObjects.productSnapshot(), 1, orderSettings);

        final Order orderWithLineItem = orderService.addOrderLineItem(createdOrder.getId(), orderLineItem);

        assertThat(orderWithLineItem.getOrderLineItems()).hasSize(1);

        final List<ProductSnapshot.ProductOptionSnapshot> productOptions = List.of(DummyObjects.productOptionSnapshot());
        UpdateLineItem updateLineItem = new UpdateLineItem(orderLineItem.getId(), 5, null, productOptions, ProductLevelOffer.GlobalProductDiscount.DISCOUNT_AMOUNT_OFF, new BigDecimal(20));

        Order updatedOrder = orderService.updateOrderLineItem(orderWithLineItem, updateLineItem);

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
            assertThat(o.getServiceCharge()).isEqualByComparingTo("47.25");
            assertThat(o.getOrderTotal()).isEqualByComparingTo(BigDecimal.valueOf((110 - 20) * 5 * 1.05 * 1.1));
        });

        updateLineItem = new UpdateLineItem(orderLineItem.getId(), 5, new BigDecimal("50"), productOptions, ProductLevelOffer.GlobalProductDiscount.NO_DISCOUNT, BigDecimal.ZERO);
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

        assertThat(updatedOrder.getOrderLineItem(orderLineItem.getId()).getLineItemSubTotal()).isEqualByComparingTo("0");
        assertThat(updatedOrder.getOrderTotal()).isEqualByComparingTo("0");
    }

    @Test
    void getOrders() {

        final LocalDateTime fromDate = LocalDateTime.now();
        final LocalDateTime toDate = fromDate.plusDays(1);
        LOGGER.info("Date range: {}, {}", fromDate, toDate);

        final Order order = new Order(client.getId(), orderSettings);
        orderService.createOrder(order);

        final ZonedDateRange zonedDateRange = ZonedDateRangeBuilder.builder(client, DateParameterType.RANGE).dateRange(fromDate, toDate).build();

        final List<Order> orders = orderService.getOrders(client, zonedDateRange);

        LOGGER.info("Orders: {}", orders);
        assertThat(orders).isNotEmpty();

        final ZonedDateRange zonedDateRange2 = ZonedDateRangeBuilder.builder(client, DateParameterType.RANGE).dateRange(toDate, toDate.plusDays(1)).build();

        final List<Order> shouldBeEmpty = orderService.getOrders(client, zonedDateRange2);
        assertThat(shouldBeEmpty).isEmpty();
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

        final OrderIdCounter shouldBeSaved = new OrderIdCounter(client.getId(), todayIdPrefix, 1);
        orderIdCounterRepository.save(shouldBeSaved);

        assertThat(shouldBeSaved.getId()).isNotNull();

        assertThat(orderService.generateSerialId(client.getId())).isEqualTo("%s-%s", todayIdPrefix, 4);

        assertThat(orderIdCounterRepository.findAll()).hasSize(2);
        assertThat(orderIdCounterRepository.findAll().get(0).getId()).isNotNull();

    }
}