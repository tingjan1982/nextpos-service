package io.nextpos.merchandising.service;

import io.nextpos.client.data.Client;
import io.nextpos.merchandising.data.Offer;
import io.nextpos.merchandising.data.OrderLevelOffer;
import io.nextpos.merchandising.data.ProductLevelOffer;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderSettings;
import io.nextpos.ordermanagement.data.ProductSnapshot;
import io.nextpos.product.data.Product;
import io.nextpos.product.data.ProductLabel;
import io.nextpos.shared.DummyObjects;
import org.assertj.core.data.Index;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;

class GroupedOffersTest {

    private Client client;

    private Order order;

    @BeforeEach
    void prepare() {

        client = DummyObjects.dummyClient();

        final BigDecimal taxRate = BigDecimal.valueOf(0.05);
        order = new Order(client.getId(), new OrderSettings(taxRate, false, Currency.getInstance("TWD"), BigDecimal.ZERO));

        final ProductSnapshot item1Product = new ProductSnapshot("item1product", "coffee", "sku", BigDecimal.valueOf(100), null);
        order.addOrderLineItem(item1Product, 2);

        final ProductSnapshot item2Product = new ProductSnapshot("item2product", "tea", "sku", BigDecimal.valueOf(75), null);
        order.addOrderLineItem(item2Product, 1);

        final ProductSnapshot item3Product = new ProductSnapshot("item3product", "coke", "sku", BigDecimal.valueOf(30), null);
        item3Product.setLabelInformation("label-id", "soft drink");
        order.addOrderLineItem(item3Product, 1);
    }

    @Test
    void arbitrateBestOffer() {

        final GroupedOffers groupedOffers = new GroupedOffers();
        final ProductLevelOffer productOffer = new ProductLevelOffer(client,
                "product offer",
                Offer.TriggerType.ALWAYS,
                Offer.DiscountType.PERCENT_OFF,
                BigDecimal.valueOf(0.1),
                true);

        groupedOffers.addProductLevelOffer(productOffer);

        final ProductLevelOffer teaOffer = new ProductLevelOffer(client,
                "tea and soft offer",
                Offer.TriggerType.ALWAYS,
                Offer.DiscountType.AMOUNT_OFF,
                BigDecimal.valueOf(10),
                false);
        final Product product = new Product(client, DummyObjects.dummyProductVersion());
        product.setId("item2product");
        teaOffer.addProduct(product);

        final ProductLabel softDrinkLabel = new ProductLabel("soft drink", client);
        softDrinkLabel.setId("label-id");
        teaOffer.addProductLabel(softDrinkLabel);

        groupedOffers.addProductLevelOffer(teaOffer);

        final OrderLevelOffer orderOffer = new OrderLevelOffer(client,
                "order offer",
                Offer.TriggerType.ALWAYS,
                Offer.DiscountType.AMOUNT_OFF,
                BigDecimal.valueOf(25));

        groupedOffers.addOrderLevelOffer(orderOffer);

        groupedOffers.arbitrateBestProductLevelOffer(order);

        assertThat(order.getOrderLineItems()).satisfies(li -> {
            assertThat(li.getSubTotal().getAmountWithoutTax()).isEqualByComparingTo(BigDecimal.valueOf(200));
            assertThat(li.getDiscountedSubTotal().getAmountWithoutTax()).isEqualByComparingTo(BigDecimal.valueOf(180));
        }, Index.atIndex(0));

        assertThat(order.getOrderLineItems()).satisfies(li -> {
            assertThat(li.getSubTotal().getAmountWithoutTax()).isEqualByComparingTo(BigDecimal.valueOf(75));
            assertThat(li.getDiscountedSubTotal().getAmountWithoutTax()).isEqualByComparingTo(BigDecimal.valueOf(65));
        }, Index.atIndex(1));

        assertThat(order.getOrderLineItems()).satisfies(li -> {
            assertThat(li.getSubTotal().getAmountWithoutTax()).isEqualByComparingTo(BigDecimal.valueOf(30));
            assertThat(li.getDiscountedSubTotal().getAmountWithoutTax()).isEqualByComparingTo(BigDecimal.valueOf(20));
        }, Index.atIndex(2));

        assertThat(order.getTotal()).satisfies(ta -> {
            assertThat(ta.getAmountWithoutTax()).isEqualByComparingTo(BigDecimal.valueOf(265));
            assertThat(ta.getAmountWithTax()).isEqualByComparingTo(BigDecimal.valueOf(278.25));
        });

        assertThat(order.getDiscountedTotal().isZero()).isTrue();

        groupedOffers.arbitrateBestOrderLevelOffer(order);

        assertThat(order.getDiscountedTotal()).satisfies(ta -> {
            assertThat(ta.getAmountWithoutTax()).isEqualByComparingTo(BigDecimal.valueOf(240));
            assertThat(ta.getAmountWithTax()).isEqualByComparingTo(BigDecimal.valueOf(252));
        });
    }
}