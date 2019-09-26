package io.nextpos.ordermanagement.service;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.ordermanagement.data.ProductSnapshot;
import io.nextpos.settings.data.CountrySettings;
import io.nextpos.shared.DummyObjects;
import org.assertj.core.data.Index;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class OrderServiceImplTest {

    private static final String CLIENT_ID = "KING";

    @Autowired
    private OrderService orderService;

    @Autowired
    private ShiftService shiftService;

    @Autowired
    private CountrySettings countrySettings;

    @BeforeEach
    void prepare() {
        shiftService.openShift(CLIENT_ID, BigDecimal.ONE);
    }

    @AfterEach
    void teardown() {
        shiftService.closeShift(CLIENT_ID, BigDecimal.ONE);
    }

    @Test
    @WithMockUser("dummyUser")
    void createAndGetOrder() {

        final Order order = new Order(CLIENT_ID, countrySettings.getTaxRate(), countrySettings.getCurrency());
        order.setTableId("table 1");

        final List<ProductSnapshot.ProductOptionSnapshot> options = List.of(
                new ProductSnapshot.ProductOptionSnapshot("ice", "1/3"),
                new ProductSnapshot.ProductOptionSnapshot("sugar", "none", BigDecimal.valueOf(10))
        );

        final ProductSnapshot product = new ProductSnapshot(UUID.randomUUID().toString(), "coffee", "tw01", BigDecimal.valueOf(100), options);
        order.addOrderLineItem(product, 1);
        order.addOrderLineItem(product, 2);

        order.setServiceCharge(BigDecimal.valueOf(0.1));
        final Order createdOrder = orderService.createOrder(order);


        assertThat(createdOrder.getId()).isNotNull();
        assertThat(createdOrder.getClientId()).isEqualTo(CLIENT_ID);
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

        final List<Order> inflightOrders = orderService.getInflightOrders(CLIENT_ID);

        assertThat(inflightOrders).hasSize(1);
    }

    @Test
    @WithMockUser("dummyUser")
    void addAndUpdateOrderLineItem() {

        final Order order = new Order(CLIENT_ID, countrySettings.getTaxRate(), countrySettings.getCurrency());
        final Order createdOrder = orderService.createOrder(order);


        final ProductSnapshot product = DummyObjects.productSnapshot();
        final OrderLineItem orderLineItem = new OrderLineItem(product, 1, countrySettings.getTaxRate());

        orderService.addOrderLineItem(createdOrder, orderLineItem);

        assertThat(createdOrder.getOrderLineItems()).hasSize(1);

        final Order updatedOrder = orderService.updateOrderLineItem(createdOrder.getId(), createdOrder.getOrderLineItems().get(0).getId(), 5);

        assertThat(updatedOrder.getOrderLineItems()).satisfies(li -> {
            assertThat(li.getQuantity()).isEqualTo(5);
        }, Index.atIndex(0));
    }

    @Test
    void copyOrder() {

        final Order order = new Order(CLIENT_ID, countrySettings.getTaxRate(), countrySettings.getCurrency());
        order.addOrderLineItem(DummyObjects.productSnapshot(), 5);

        final ProductSnapshot productWithOption = new ProductSnapshot("pid",
                "custom",
                null,
                BigDecimal.valueOf(200),
                List.of(new ProductSnapshot.ProductOptionSnapshot("ice", "normal", BigDecimal.valueOf(5))));
        final OrderLineItem lineItem = new OrderLineItem(productWithOption, 2, countrySettings.getTaxRate());
        order.addOrderLineItem(lineItem);

        orderService.createOrder(order);

        final Order copiedOrder = orderService.copyOrder(order.getId());

        assertThat(copiedOrder.getMetadata(Order.COPY_FROM_ORDER)).isEqualTo(order.getId());
        assertThat(copiedOrder).isEqualToIgnoringGivenFields(order, "id", "orderLineItems", "metadata", "internalCounter", "createdDate", "modifiedDate");
        assertThat(copiedOrder.getOrderLineItems()).usingElementComparatorIgnoringFields("id").isEqualTo(order.getOrderLineItems());
    }
}