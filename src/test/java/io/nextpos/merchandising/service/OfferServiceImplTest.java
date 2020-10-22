package io.nextpos.merchandising.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.datetime.service.ZonedDateRangeBuilder;
import io.nextpos.merchandising.data.Offer;
import io.nextpos.merchandising.data.OfferType;
import io.nextpos.merchandising.data.OrderLevelOffer;
import io.nextpos.merchandising.data.ProductLevelOffer;
import io.nextpos.product.data.Product;
import io.nextpos.product.data.ProductLabel;
import io.nextpos.product.service.ProductLabelService;
import io.nextpos.product.service.ProductService;
import io.nextpos.reporting.data.DateParameterType;
import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.exception.ConfigurationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class OfferServiceImplTest {

    @Autowired
    private OfferService offerService;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductLabelService productLabelService;

    @Autowired
    private ClientService clientService;

    private Client client;

    @BeforeEach
    void prepare() {
        client = DummyObjects.dummyClient();
        clientService.saveClient(client);
    }

    @Test
    void saveOrderLevelOffer() {

        final OrderLevelOffer orderLevelOffer = new OrderLevelOffer(client, "order level promotion", Offer.TriggerType.ALWAYS, Offer.DiscountType.PERCENT_OFF, BigDecimal.valueOf(0.15));

        offerService.saveOffer(orderLevelOffer);

        assertThat(orderLevelOffer.getId()).isNotNull();
        assertThat(orderLevelOffer.getTriggerType()).isEqualTo(Offer.TriggerType.ALWAYS);
        assertThat(orderLevelOffer.getDiscountDetails()).satisfies(dd -> {
            assertThat(dd.getDiscountType()).isEqualTo(Offer.DiscountType.PERCENT_OFF);
            assertThat(dd.getDiscountValue()).isEqualTo(BigDecimal.valueOf(0.15));
        });

        assertThatThrownBy(() -> new OrderLevelOffer(client, null, Offer.TriggerType.ALWAYS, Offer.DiscountType.AMOUNT, BigDecimal.ZERO)).isInstanceOf(ConfigurationException.class);
    }

    @Test
    void checkOfferActivation() {

        final OrderLevelOffer orderLevelOffer = new OrderLevelOffer(client, "order level promotion", Offer.TriggerType.ALWAYS, Offer.DiscountType.PERCENT_OFF, BigDecimal.valueOf(0.15));

        offerService.saveOffer(orderLevelOffer);

        final Offer retrievedOffer = offerService.getOffer(orderLevelOffer.getId());

        assertThat(retrievedOffer.isActive()).isFalse();

        offerService.activateOffer(retrievedOffer);

        assertThat(retrievedOffer.isActive()).isTrue();

        final Date now = Date.from(Instant.now().minusSeconds(5));
        final Date tomorrow = Date.from(Instant.now().plus(1, ChronoUnit.DAYS));
        final Date yesterday = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));

        retrievedOffer.updateOfferEffectiveDetails(true, now, tomorrow);
        offerService.saveOffer(retrievedOffer);

        assertThat(retrievedOffer.isActive()).isTrue();

        retrievedOffer.updateOfferEffectiveDetails(true, tomorrow, tomorrow);
        offerService.saveOffer(retrievedOffer);

        assertThat(retrievedOffer.isActive()).isFalse();

        retrievedOffer.updateOfferEffectiveDetails(true, now, yesterday);
        offerService.saveOffer(retrievedOffer);

        assertThat(retrievedOffer.isActive()).isFalse();

        retrievedOffer.updateOfferEffectiveDetails(false, null, null);
        offerService.saveOffer(retrievedOffer);

        assertThat(retrievedOffer.isActive()).isFalse();
    }

    @Test
    void saveProductLevelOffer() {

        final Product product = new Product(client, DummyObjects.dummyProductVersion());
        productService.saveProduct(product);

        final ProductLabel drink = new ProductLabel("drink", client);
        productLabelService.saveProductLabel(drink);

        final ProductLevelOffer productLevelOffer = new ProductLevelOffer(client, "product level promotion", Offer.TriggerType.MEMBER, Offer.DiscountType.AMOUNT_OFF, BigDecimal.valueOf(50), false);
        productLevelOffer.addProduct(product);
        productLevelOffer.addProductLabel(drink);

        offerService.saveOffer(productLevelOffer);

        assertThat(productLevelOffer.getId()).isNotNull();
        assertThat(productLevelOffer.getTriggerType()).isEqualTo(Offer.TriggerType.MEMBER);
        assertThat(productLevelOffer.getDiscountDetails()).satisfies(dd -> {
            assertThat(dd.getDiscountType()).isEqualTo(Offer.DiscountType.AMOUNT_OFF);
            assertThat(dd.getDiscountValue()).isEqualTo(BigDecimal.valueOf(50));
        });
        assertThat(productLevelOffer.getAppliesToProducts()).hasSize(1);
        assertThat(productLevelOffer.getAppliesToProductLabels()).hasSize(1);
    }

    @Test
    void findOffers() {

        final OrderLevelOffer orderLevelOffer = new OrderLevelOffer(client, "order level promotion", Offer.TriggerType.ALWAYS, Offer.DiscountType.PERCENT_OFF, BigDecimal.valueOf(0.15));
        offerService.saveOffer(orderLevelOffer);
        offerService.activateOffer(orderLevelOffer);

        final OrderLevelOffer inactiveOrderLevelOffer = new OrderLevelOffer(client, "order level promotion", Offer.TriggerType.ALWAYS, Offer.DiscountType.PERCENT_OFF, BigDecimal.valueOf(0.15));
        final ZonedDateRange zonedDateRange = ZonedDateRangeBuilder.builder(client, DateParameterType.RANGE)
                .dateRange(LocalDateTime.now().minusHours(12), LocalDateTime.now().minusHours(6)).build();
        inactiveOrderLevelOffer.updateOfferEffectiveDetails(true, zonedDateRange.getFromDate(), zonedDateRange.getToDate());
        offerService.saveOffer(inactiveOrderLevelOffer);

        final ProductLevelOffer productLevelOffer = new ProductLevelOffer(client, "product level promotion", Offer.TriggerType.ALWAYS, Offer.DiscountType.AMOUNT_OFF, BigDecimal.valueOf(50), false);
        offerService.saveOffer(productLevelOffer);
        offerService.activateOffer(productLevelOffer);

        final GroupedOffers activeOffers = offerService.findActiveOffers(client);

        assertThat(activeOffers.getOrderLevelOffers()).hasSize(1);
        assertThat(activeOffers.getProductLevelOffers()).hasSize(1);

        final List<Offer> offers = offerService.getOffers(client);

        assertThat(offers).hasSize(3);
        assertThat(offers.get(0)).isInstanceOf(OrderLevelOffer.class);
        assertThat(offers.get(1)).isInstanceOf(OrderLevelOffer.class);
        assertThat(offers.get(2)).isInstanceOf(ProductLevelOffer.class);

        final List<? extends Offer> orderOffers = offerService.getActiveOffers(client, OfferType.ORDER, Offer.TriggerType.ALWAYS);
        assertThat(orderOffers).hasSize(1);

        final List<? extends Offer> productOffers = offerService.getActiveOffers(client, OfferType.PRODUCT, Offer.TriggerType.ALWAYS);
        assertThat(productOffers).hasSize(1);
    }
}