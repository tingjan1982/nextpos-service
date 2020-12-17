package io.nextpos.membership.service;

import io.nextpos.client.data.Client;
import io.nextpos.membership.data.Membership;
import io.nextpos.membership.data.OrderTopRanking;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderSettings;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.settings.data.CountrySettings;
import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ChainedTransaction
class MembershipReportServiceImplTest {

    private final MembershipReportService membershipReportService;

    private final MembershipService membershipService;

    private final OrderService orderService;

    private final Client client;

    private final CountrySettings countrySettings;

    private Membership membership;

    @Autowired
    MembershipReportServiceImplTest(MembershipReportService membershipReportService, MembershipService membershipService, OrderService orderService, Client client, CountrySettings countrySettings) {
        this.membershipReportService = membershipReportService;
        this.membershipService = membershipService;
        this.orderService = orderService;
        this.client = client;
        this.countrySettings = countrySettings;
    }

    @BeforeEach
    void prepare() {
        OrderSettings orderSettings = new OrderSettings(countrySettings, true, BigDecimal.ZERO);
        membership = new Membership(client.getId(), "rain", "1234567890");
        membershipService.saveMembership(membership);

        for (int i = 0; i < 10; i++) {
            final Order order = Order.newOrder(client.getId(), Order.OrderType.IN_STORE, orderSettings);
            order.addOrderLineItem(DummyObjects.productSnapshot("coffee", new BigDecimal("100")), 5);
            order.addOrderLineItem(DummyObjects.productSnapshot("tea", new BigDecimal("100")), 4);
            order.addOrderLineItem(DummyObjects.productSnapshot("pepsi", new BigDecimal("100")), 3);
            order.addOrderLineItem(DummyObjects.productSnapshot("coke", new BigDecimal("100")), 2);
            order.addOrderLineItem(DummyObjects.productSnapshot("root beer", new BigDecimal("100")), 1);
            order.addOrderLineItem(DummyObjects.productSnapshot("shit", new BigDecimal("100")), 1);
            order.updateMembership(membership);

            orderService.saveOrder(order);
        }
    }

    @Test
    void getRecentOrders() {

        final List<Order> lastFiveOrders = membershipReportService.getRecentOrders(membership, 5);

        assertThat(lastFiveOrders).hasSize(5);
    }

    @Test
    void getTopFiveOrderLineItems() {

        final List<OrderTopRanking> topRankingOrderLineItems = membershipReportService.getTopRankingOrderLineItems(membership, 3);

        assertThat(topRankingOrderLineItems).hasSize(3);
    }
}