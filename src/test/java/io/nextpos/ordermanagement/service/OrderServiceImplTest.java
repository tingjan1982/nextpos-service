package io.nextpos.ordermanagement.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.merchandising.data.ProductLevelOffer;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.ordermanagement.data.OrderSettings;
import io.nextpos.ordermanagement.data.ProductSnapshot;
import io.nextpos.ordermanagement.service.bean.UpdateLineItem;
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

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private ShiftService shiftService;

    @Autowired
    private OrderSettings orderSettings;

    @Autowired
    private TableLayoutService tableLayoutService;

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
        order.setTableInfo(new Order.TableInfo(tableDetails));

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

        orderService.addOrderLineItem(createdOrder, orderLineItem);

        assertThat(createdOrder.getOrderLineItems()).hasSize(1);

        final List<ProductSnapshot.ProductOptionSnapshot> productOptions = List.of(DummyObjects.productOptionSnapshot());
        final UpdateLineItem updateLineItem = new UpdateLineItem(orderLineItem.getId(), 5, productOptions, ProductLevelOffer.GlobalProductDiscount.DISCOUNT_AMOUNT_OFF, new BigDecimal(20));

        final Order updatedOrder = orderService.updateOrderLineItem(createdOrder, updateLineItem);

        assertThat(updatedOrder.getOrderLineItems()).satisfies(li -> {
            assertThat(li.getQuantity()).isEqualTo(5);
            assertThat(li.getProductPriceWithOptions().getAmountWithoutTax()).isEqualByComparingTo("110");
            assertThat(li.getProductPriceWithOptions().getAmountWithTax()).isEqualByComparingTo("115.5");
            assertThat(li.getSubTotal().getAmount()).isEqualByComparingTo("550");
            assertThat(li.getDiscountedSubTotal().getAmount()).isEqualByComparingTo("450");
        }, Index.atIndex(0));

        assertThat(updatedOrder).satisfies(o -> {
            assertThat(o.getOrderTotal()).isEqualByComparingTo(BigDecimal.valueOf((110 - 20) * 5 * 1.05 * 1.1));
            assertThat(o.getServiceCharge()).isEqualByComparingTo(o.getTotal().getAmountWithTax().multiply(new BigDecimal("0.1")));
        });
    }

    @Test
    void getOrders() {

        final LocalDateTime fromDate = LocalDateTime.now();
        final LocalDateTime toDate = fromDate.plusDays(1);
        LOGGER.info("Date range: {}, {}", fromDate, toDate);

        final Order order = new Order(client.getId(), orderSettings);
        orderService.createOrder(order);

        final List<Order> orders = orderService.getOrders(client, fromDate, toDate);

        LOGGER.info("Orders: {}", orders);
        assertThat(orders).isNotEmpty();

        final List<Order> shouldBeEmpty = orderService.getOrders(client, toDate, toDate.plusDays(1));
        assertThat(shouldBeEmpty).isEmpty();
    }

    @Test
    void copyOrder() {

        final Order order = new Order(client.getId(), orderSettings);
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

        assertThat(copiedOrder.getMetadata(Order.COPY_FROM_ORDER)).isEqualTo(order.getId());
        assertThat(copiedOrder).isEqualToIgnoringGivenFields(order, "id", "lookupOrderId", "orderLineItems", "metadata", "internalCounter", "createdDate", "modifiedDate");
        assertThat(copiedOrder.getOrderLineItems()).usingElementComparatorIgnoringFields("id", "createdDate", "modifiedDate").isEqualTo(order.getOrderLineItems());
    }
}