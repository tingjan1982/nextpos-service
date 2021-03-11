package io.nextpos.merchandising.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.merchandising.data.Offer;
import io.nextpos.merchandising.data.OrderLevelOffer;
import io.nextpos.merchandising.data.ProductLevelOffer;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.ordermanagement.data.OrderSettings;
import io.nextpos.ordermanagement.data.ProductSnapshot;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.exception.BusinessLogicException;
import org.assertj.core.data.Index;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class MerchandisingServiceImplTest {

    @Autowired
    private MerchandisingService merchandisingService;

    @Autowired
    private OfferService offerService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private OrderSettings orderSettings;

    private Client client;


    @BeforeEach
    void prepare() {
        client = DummyObjects.dummyClient();
        clientService.saveClient(client);

        final OrderLevelOffer orderDiscount = new OrderLevelOffer(client, "order discount", Offer.TriggerType.ALWAYS, Offer.DiscountType.PERCENT_OFF, BigDecimal.valueOf(.1));
        offerService.activateOffer(orderDiscount);

        final OrderLevelOffer orderDiscount2 = new OrderLevelOffer(client, "order discount", Offer.TriggerType.ALWAYS, Offer.DiscountType.PERCENT_OFF, BigDecimal.valueOf(.2));
        offerService.activateOffer(orderDiscount2);

        final ProductLevelOffer productDiscount = new ProductLevelOffer(client, "product discount", Offer.TriggerType.ALWAYS, Offer.DiscountType.PERCENT_OFF, BigDecimal.valueOf(0.05), true);
        offerService.activateOffer(productDiscount);

        final ProductLevelOffer productDiscount2 = new ProductLevelOffer(client, "product discount", Offer.TriggerType.ALWAYS, Offer.DiscountType.PERCENT_OFF, BigDecimal.valueOf(0.01), true);
        offerService.activateOffer(productDiscount2);
    }

    @Test
    void computeOffers() {

        final OrderSettings taiwanOrderSettings = new OrderSettings(BigDecimal.valueOf(0.05), true, Currency.getInstance("TWD"), BigDecimal.ZERO, 0, RoundingMode.HALF_UP);
        Order order = new Order(client.getId(), taiwanOrderSettings);
        orderService.saveOrder(order);

        final OrderLineItem lineItem = new OrderLineItem(DummyObjects.productSnapshot(), 1, orderSettings);

        order = orderService.addOrderLineItem(client, order, lineItem);

        assertThat(order.getOrderLineItems()).satisfies(li -> {
            assertThat(li.getProductSnapshot().getPrice()).isEqualByComparingTo("100");
            assertThat(li.getProductSnapshot().getProductPriceWithOptions()).isEqualByComparingTo("100");
            assertThat(li.getProductSnapshot().getDiscountedPrice()).isEqualByComparingTo("95");
            assertThat(li.getLineItemSubTotal()).isEqualByComparingTo("95");
            assertThat(li.getDiscountedSubTotal().getAmount()).isEqualByComparingTo("95");
        }, Index.atIndex(0));

        assertThat(order.getTotal().getAmount()).isEqualByComparingTo("95");
        assertThat(order.getDiscountedTotal().getAmount()).isEqualByComparingTo("76");
        assertThat(order.getOrderTotal()).isEqualByComparingTo("76");

        merchandisingService.applyOrderOffer(order, OrderLevelOffer.GlobalOrderDiscount.DISCOUNT_AMOUNT_OFF.name(), BigDecimal.valueOf(50));

        assertThat(order.getTotal().getAmount()).isEqualByComparingTo("95");
        assertThat(order.getDiscountedTotal().getAmount()).isEqualByComparingTo("45");
        assertThat(order.getOrderTotal()).isEqualByComparingTo("45");

        order = orderService.addOrderLineItem(client, order, lineItem);

        assertThat(order.getTotal().getAmount()).isEqualByComparingTo("190");
        assertThat(order.getDiscountedTotal().getAmount()).isEqualByComparingTo("152");
        assertThat(order.getOrderTotal()).isEqualByComparingTo("152");

        order = orderService.updateOrderLineItemPrice(order, order.getOrderLineItems().get(0).getId(), BigDecimal.valueOf(30));

        assertThat(order.getTotal().getAmount()).isEqualByComparingTo("60");
        assertThat(order.getDiscountedTotal().getAmount()).isEqualByComparingTo("48");
        assertThat(order.getOrderTotal()).isEqualByComparingTo("48");
    }

    @Test
    void applyGlobalOrderDiscount() {

        final Order order = new Order(client.getId(), orderSettings);
        order.addOrderLineItem(DummyObjects.productSnapshot(), 1);

        final Order updatedOrder = merchandisingService.applyOrderOffer(order, OrderLevelOffer.GlobalOrderDiscount.ENTER_DISCOUNT.name(), BigDecimal.valueOf(20));

        assertThat(updatedOrder.getTotal().getAmountWithTax()).isEqualByComparingTo(BigDecimal.valueOf(105));
        assertThat(updatedOrder.getDiscountedTotal().getAmountWithTax()).isEqualByComparingTo(BigDecimal.valueOf(84));
        assertThat(updatedOrder.getAppliedOfferInfo()).isNotNull();
    }

    @Test
    void applyOrderDiscount() {

        final Order order = new Order(client.getId(), orderSettings);
        order.addOrderLineItem(DummyObjects.productSnapshot(), 1);

        final OrderLevelOffer orderLevelOffer = new OrderLevelOffer(client, "order offer", Offer.TriggerType.AT_CHECKOUT, Offer.DiscountType.AMOUNT_OFF, BigDecimal.TEN);
        offerService.saveOffer(orderLevelOffer);

        assertThatThrownBy(() -> merchandisingService.applyOrderOffer(order, orderLevelOffer.getId(), BigDecimal.ZERO))
                .isInstanceOf(BusinessLogicException.class);

        offerService.activateOffer(orderLevelOffer);

        final Order updatedOrder = merchandisingService.applyOrderOffer(order, orderLevelOffer.getId(), BigDecimal.ZERO);

        assertThat(updatedOrder.getTotal().getAmountWithTax()).isEqualByComparingTo(BigDecimal.valueOf(105));
        assertThat(updatedOrder.getDiscountedTotal().getAmountWithTax()).isEqualByComparingTo(BigDecimal.valueOf(94.5));
        assertThat(updatedOrder.getDiscount()).isEqualByComparingTo("10.5");
        assertThat(updatedOrder.getOrderTotal()).isEqualByComparingTo("105");
        assertThat(updatedOrder.getAppliedOfferInfo().getOfferId()).isEqualTo(orderLevelOffer.getId());

        final Order orderWithOfferRemoved = merchandisingService.removeOrderOffer(updatedOrder);

        assertThat(orderWithOfferRemoved.getTotal().getAmountWithTax()).isEqualByComparingTo(BigDecimal.valueOf(105));
        assertThat(orderWithOfferRemoved.getDiscountedTotal().getAmountWithTax()).isZero();
        assertThat(orderWithOfferRemoved.getDiscount()).isZero();
        assertThat(orderWithOfferRemoved.getOrderTotal()).isEqualByComparingTo("115.5");
        assertThat(orderWithOfferRemoved.getAppliedOfferInfo()).isNull();
    }

    @Test
    void applyOrderDiscount_GlobalOrderDiscount() {

        final Order order = new Order(client.getId(), orderSettings);
        order.addOrderLineItem(DummyObjects.productSnapshot(), 1);

        final OrderLevelOffer.GlobalOrderDiscount globalDiscount = OrderLevelOffer.GlobalOrderDiscount.ENTER_DISCOUNT;
        final Order updatedOrder = merchandisingService.applyOrderOffer(order, globalDiscount.name(), new BigDecimal(10));

        assertThat(updatedOrder.getTotal().getAmountWithTax()).isEqualByComparingTo(BigDecimal.valueOf(105));
        assertThat(updatedOrder.getDiscountedTotal().getAmountWithTax()).isEqualByComparingTo(BigDecimal.valueOf(94.5));
        assertThat(updatedOrder.getDiscount()).isEqualByComparingTo("10.5");
        assertThat(updatedOrder.getOrderTotal()).isEqualByComparingTo("105");
        assertThat(updatedOrder.getAppliedOfferInfo().getOfferId()).isEqualTo(globalDiscount.name());
    }

    @Test
    void applyProductDiscount() {

        final Order order = new Order(client.getId(), orderSettings);
        final ProductSnapshot productSnapshot = DummyObjects.productSnapshot();
        productSnapshot.setOverridePrice(new BigDecimal("50"));
        final OrderLineItem orderLineItem = order.addOrderLineItem(productSnapshot, 1);

        final OrderLineItem updatedOrderLineItem = merchandisingService.applyGlobalProductDiscount(orderLineItem, ProductLevelOffer.GlobalProductDiscount.DISCOUNT_AMOUNT_OFF, BigDecimal.valueOf(15));

        assertThat(updatedOrderLineItem.getProductSnapshot()).satisfies(p -> {
            assertThat(p.getPrice()).isEqualByComparingTo("100");
            assertThat(p.getOverridePrice()).isEqualByComparingTo("50");
            assertThat(p.getDiscountedPrice()).isEqualByComparingTo("35");
            assertThat(p.getProductPriceWithOptions()).isEqualByComparingTo("50");
        });

        assertThat(updatedOrderLineItem).satisfies(li -> {
            assertThat(li.getSubTotal().getAmount()).isEqualByComparingTo("50");
            assertThat(li.getDiscountedSubTotal().getAmount()).isEqualByComparingTo("35");
            assertThat(li.getLineItemSubTotal()).isEqualByComparingTo("35");
        });
    }

    @Test
    void updateServiceCharge() {

        final Order order = new Order(client.getId(), orderSettings);
        order.addOrderLineItem(DummyObjects.productSnapshot(), 1);

        assertThat(order.getTotal().getAmountWithTax()).isEqualByComparingTo(BigDecimal.valueOf(105));
        assertThat(order.getServiceCharge()).isEqualByComparingTo(BigDecimal.valueOf(10.5));
        assertThat(order.getOrderTotal()).isEqualByComparingTo(BigDecimal.valueOf(115.5));

        final Order updatedOrder = merchandisingService.updateServiceCharge(order, true);

        assertThat(updatedOrder.getServiceCharge()).isEqualByComparingTo(BigDecimal.valueOf(0));
        assertThat(updatedOrder.getOrderTotal()).isEqualByComparingTo(BigDecimal.valueOf(105));

    }

    @Test
    void resetOrderOffers() {

        final Order order = new Order(client.getId(), orderSettings);
        order.addOrderLineItem(DummyObjects.productSnapshot(), 1);

        merchandisingService.applyOrderOffer(order, OrderLevelOffer.GlobalOrderDiscount.ENTER_DISCOUNT.name(), BigDecimal.valueOf(0.2));
        merchandisingService.updateServiceCharge(order, true);

        final Order updatedOrder = merchandisingService.resetOrderOffers(order);

        assertThat(updatedOrder.getTotal().getAmountWithTax()).isEqualByComparingTo(BigDecimal.valueOf(105));
        assertThat(updatedOrder.getServiceCharge()).isEqualByComparingTo(BigDecimal.valueOf(10.5));
        assertThat(updatedOrder.getOrderTotal()).isEqualByComparingTo(BigDecimal.valueOf(115.5));
    }
}