package io.nextpos.merchandising.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientRepository;
import io.nextpos.merchandising.data.Offer;
import io.nextpos.merchandising.data.OrderLevelOffer;
import io.nextpos.merchandising.data.ProductLevelOffer;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderSettings;
import io.nextpos.shared.DummyObjects;
import org.assertj.core.data.Index;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MerchandisingServiceImplTest {

    @Autowired
    private MerchandisingService merchandisingService;

    @Autowired
    private OfferService offerService;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private OrderSettings orderSettings;

    private Client client;


    @BeforeEach
    void prepare() {
        client = DummyObjects.dummyClient();
        clientRepository.save(client);

        final OrderLevelOffer orderDiscount = new OrderLevelOffer(client, "order discount", Offer.TriggerType.ALWAYS, Offer.DiscountType.PERCENT_OFF, BigDecimal.valueOf(.1));
        offerService.activateOffer(orderDiscount);

        final ProductLevelOffer productDiscount = new ProductLevelOffer(client, "product discount", Offer.TriggerType.ALWAYS, Offer.DiscountType.PERCENT_OFF, BigDecimal.valueOf(0.05), true);
        offerService.activateOffer(productDiscount);
    }

    @Test
    void computeOffers() {

        final Order order = new Order(client.getId(), orderSettings);
        order.addOrderLineItem(DummyObjects.productSnapshot(), 1);

        merchandisingService.computeOffers(client, order);

        assertThat(order.getId()).isNotNull();
        assertThat(order.getOrderLineItems()).satisfies(li -> {
            assertThat(li.getProductSnapshot().getPrice()).isEqualByComparingTo(BigDecimal.valueOf(100));
            assertThat(li.getProductSnapshot().getProductPriceWithOptions()).isEqualByComparingTo(BigDecimal.valueOf(100));
            assertThat(li.getProductSnapshot().getDiscountedPrice()).isEqualByComparingTo(BigDecimal.valueOf(95));
            assertThat(li.getSubTotal().getAmountWithoutTax()).isEqualByComparingTo(BigDecimal.valueOf(100));
            assertThat(li.getDiscountedSubTotal().getAmountWithoutTax()).isEqualByComparingTo(BigDecimal.valueOf(95));
        }, Index.atIndex(0));

        assertThat(order.getTotal().getAmountWithoutTax()).isEqualByComparingTo(BigDecimal.valueOf(95));
        assertThat(order.getDiscountedTotal().getAmountWithoutTax()).isEqualByComparingTo(BigDecimal.valueOf(85.5));
    }

    @Test
    void applyOrderDiscount() {

        final Order order = new Order(client.getId(), orderSettings);
        order.addOrderLineItem(DummyObjects.productSnapshot(), 1);

        final Order updatedOrder = merchandisingService.applyGlobalOrderDiscount(order, OrderLevelOffer.GlobalOrderDiscount.ENTER_DISCOUNT, BigDecimal.valueOf(0.2));

        assertThat(updatedOrder.getTotal().getAmountWithTax()).isEqualByComparingTo(BigDecimal.valueOf(105));
        assertThat(updatedOrder.getDiscountedTotal().getAmountWithTax()).isEqualByComparingTo(BigDecimal.valueOf(84));
        assertThat(updatedOrder.getAppliedOfferInfo()).isNotNull();
    }

    @Test
    void updateServiceCharge() {

        final Order order = new Order(client.getId(), orderSettings);
        order.addOrderLineItem(DummyObjects.productSnapshot(), 1);

        assertThat(order.getTotal().getAmountWithTax()).isEqualByComparingTo(BigDecimal.valueOf(105));
        assertThat(order.getServiceCharge()).isEqualByComparingTo(BigDecimal.valueOf(10.5));
        assertThat(order.getOrderTotal()).isEqualByComparingTo(BigDecimal.valueOf(115.5));

        final Order updatedOrder = merchandisingService.updateServiceCharge(order, false);

        assertThat(updatedOrder.getServiceCharge()).isEqualByComparingTo(BigDecimal.valueOf(0));
        assertThat(updatedOrder.getOrderTotal()).isEqualByComparingTo(BigDecimal.valueOf(105));

    }

    @Test
    void resetOrderOffers() {

        final Order order = new Order(client.getId(), orderSettings);
        order.addOrderLineItem(DummyObjects.productSnapshot(), 1);

        merchandisingService.applyGlobalOrderDiscount(order, OrderLevelOffer.GlobalOrderDiscount.ENTER_DISCOUNT, BigDecimal.valueOf(0.2));
        merchandisingService.updateServiceCharge(order, true);

        final Order updatedOrder = merchandisingService.resetOrderOffers(order);

        assertThat(updatedOrder.getTotal().getAmountWithTax()).isEqualByComparingTo(BigDecimal.valueOf(105));
        assertThat(updatedOrder.getServiceCharge()).isEqualByComparingTo(BigDecimal.valueOf(10.5));
        assertThat(updatedOrder.getOrderTotal()).isEqualByComparingTo(BigDecimal.valueOf(115.5));
    }
}