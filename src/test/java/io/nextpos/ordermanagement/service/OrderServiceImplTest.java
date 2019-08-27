package io.nextpos.ordermanagement.service;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.ordermanagement.data.ProductSnapshot;
import io.nextpos.ordermanagement.web.model.UpdateOrderLineItemRequest;
import io.nextpos.shared.DummyObjects;
import org.assertj.core.data.Index;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@Transactional
class OrderServiceImplTest {

    @Autowired
    private OrderService orderService;


    @Test
    void createAndGetOrder() {

        BigDecimal taxRate = new BigDecimal("0.05");
        final Order order = new Order("client-id", taxRate, Currency.getInstance("TWD"));

        final List<ProductSnapshot.ProductOptionSnapshot> options = List.of(
                new ProductSnapshot.ProductOptionSnapshot("ice", "1/3"),
                new ProductSnapshot.ProductOptionSnapshot("sugar", "none", BigDecimal.valueOf(10))
        );

        final ProductSnapshot product = new ProductSnapshot(UUID.randomUUID().toString(), "coffee", "tw01", BigDecimal.valueOf(100), options);
        final OrderLineItem lineItem = new OrderLineItem(product, 1, taxRate);
        final OrderLineItem lineItem2 = new OrderLineItem(product, 2, taxRate);
        order.addOrderLineItem(lineItem).addOrderLineItem(lineItem2);

        final Order createdOrder = orderService.createOrder(order);

        assertThat(createdOrder.getId()).isNotNull();
        assertThat(createdOrder.getClientId()).isEqualTo("client-id");
        assertThat(createdOrder.getState()).isEqualTo(Order.OrderState.OPEN);
        assertThat(createdOrder.getTotal()).satisfies(total -> {
            assertThat(total.getAmountWithoutTax()).isEqualByComparingTo(new BigDecimal("330"));
            assertThat(total.getAmountWithTax()).isEqualByComparingTo(new BigDecimal("346.5"));
            assertThat(total.getTax()).isEqualByComparingTo(new BigDecimal("16.5"));
        });

        assertThat(createdOrder.getOrderLineItems()).hasSize(2);
        assertThat(createdOrder.getOrderLineItems()).satisfies(li -> {
            assertThat(li.getState()).isEqualTo(OrderLineItem.OrderLineItemState.OPEN);
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
    }

    @Test
    public void addAndUpdateOrderLineItem() {

        final BigDecimal taxRate = BigDecimal.valueOf(0.05);
        final Order order = new Order("KING", taxRate, Currency.getInstance("TWD"));
        final Order createdOrder = orderService.createOrder(order);


        final ProductSnapshot product = DummyObjects.productSnapshot();
        final OrderLineItem orderLineItem = new OrderLineItem(product, 1, taxRate);

        orderService.addOrderLineItem(createdOrder, orderLineItem);

        assertThat(createdOrder.getOrderLineItems()).hasSize(1);

        final UpdateOrderLineItemRequest updateRequest = new UpdateOrderLineItemRequest(5, List.of());
        final Order updatedOrder = orderService.updateOrderLineItem(createdOrder.getId(), createdOrder.getOrderLineItems().get(0).getId(), updateRequest);

        assertThat(updatedOrder.getOrderLineItems()).satisfies(li -> {
            assertThat(li.getQuantity()).isEqualTo(5);
        }, Index.atIndex(0));
    }
}