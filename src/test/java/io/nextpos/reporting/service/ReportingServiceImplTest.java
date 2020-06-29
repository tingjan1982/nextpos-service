package io.nextpos.reporting.service;

import io.nextpos.client.data.Client;
import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.datetime.service.ZonedDateRangeBuilder;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderSettings;
import io.nextpos.ordermanagement.data.OrderStateChange;
import io.nextpos.ordermanagement.data.ProductSnapshot;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.reporting.data.DateParameterType;
import io.nextpos.reporting.data.OrderStateAverageTimeReport;
import io.nextpos.reporting.data.OrderStateParameter;
import io.nextpos.shared.DummyObjects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ReportingServiceImplTest {

    @Autowired
    private ReportingService reportingService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private OrderSettings orderSettings;

    private Client client;

    @BeforeEach
    void prepare() {
        final String clientId = ReportingServiceImplTest.class.getSimpleName();
        client = DummyObjects.dummyClient();
        client.setId(clientId);
    }

    @Test
    public void generateOrderStateAverageTimeReport() {

        final List<Order> orders = List.of(
                this.createAndTransitionOrderToDelivered(client.getId()),
                this.createAndTransitionOrderToDelivered(client.getId()));

        final ZonedDateRange zonedDateRange = ZonedDateRangeBuilder.builder(client, DateParameterType.MONTH).build();
        final OrderStateParameter orderStateParameter = new OrderStateParameter(
                zonedDateRange,
                Order.OrderState.OPEN,
                Order.OrderState.DELIVERED
        );
        final OrderStateAverageTimeReport report = reportingService.generateStateTransitionAverageTimeReport(client, orderStateParameter);

        assertThat(report).isNotNull();

        orders.forEach(order -> {
            mongoTemplate.remove(Query.query(Criteria.where("_id").is(order.getId())), Order.class);
            mongoTemplate.remove(Query.query(Criteria.where("_id").is(order.getId())), OrderStateChange.class);
        });
    }

    private Order createAndTransitionOrderToDelivered(String clientId) {

        final Order order = this.createOrder(clientId);
        orderService.transitionOrderState(order, Order.OrderAction.SUBMIT, Optional.empty());
        orderService.transitionOrderState(order, Order.OrderAction.DELIVER, Optional.empty());

        return order;
    }

    private Order createOrder(final String clientId) {

        final BigDecimal taxRate = BigDecimal.ZERO;
        final Order order = new Order(clientId, orderSettings);
        final ProductSnapshot product = DummyObjects.productSnapshot();
        order.addOrderLineItem(product, 5);

        return orderService.createOrder(order);
    }
}