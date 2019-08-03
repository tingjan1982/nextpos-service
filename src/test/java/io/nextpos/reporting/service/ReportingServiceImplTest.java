package io.nextpos.reporting.service;

import io.nextpos.client.data.Client;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.ordermanagement.data.OrderStateChange;
import io.nextpos.ordermanagement.data.ProductSnapshot;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.reporting.data.DateParameterType;
import io.nextpos.reporting.data.OrderStateAverageTimeReport;
import io.nextpos.reporting.data.OrderStateParameter;
import io.nextpos.reporting.data.SalesReport;
import io.nextpos.shared.DummyObjects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@Transactional
class ReportingServiceImplTest {

    @Autowired
    private ReportingService reportingService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private MongoTemplate mongoTemplate;


    @Test
    void generateSalesReport() {

        final String clientId = ReportingServiceImplTest.class.getSimpleName();
        final Client client = DummyObjects.dummyClient();
        client.setId(clientId);

        final Order createdOrder = this.createOrder(clientId);

        final SalesReport salesReport = reportingService.generateSalesReport(client, DateParameterType.TODAY.toReportingParameter());

        assertThat(salesReport).isNotNull();
        assertThat(salesReport.getSalesTotal()).isGreaterThan(BigDecimal.ZERO);
        assertThat(salesReport.getOrderCount()).isGreaterThan(0);

        orderService.deleteOrder(createdOrder);
    }

    @Test
    public void generateOrderStateAverageTimeReport() {

        final String clientId = ReportingServiceImplTest.class.getSimpleName();
        final Client client = DummyObjects.dummyClient();
        client.setId(clientId);

        final List<Order> orders = List.of(this.createAndTransitionOrderToDelivered(clientId), this.createAndTransitionOrderToDelivered(clientId));

        final OrderStateParameter orderStateParameter = new OrderStateParameter(
                DateParameterType.TODAY.toReportingParameter(),
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
        orderService.transitionOrderState(order, Order.OrderState.IN_PROCESS);
        orderService.transitionOrderState(order, Order.OrderState.DELIVERED);

        return order;
    }

    private Order createOrder(final String clientId) {

        final BigDecimal taxRate = BigDecimal.ZERO;
        final Order order = new Order(clientId, taxRate);
        final ProductSnapshot product = DummyObjects.productSnapshot();
        final OrderLineItem orderLineItem = new OrderLineItem(product, 5, taxRate);
        order.addOrderLineItem(orderLineItem);

        return orderService.createOrder(order);
    }
}