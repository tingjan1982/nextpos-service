package io.nextpos.reporting.service;

import io.nextpos.client.data.Client;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.ordermanagement.data.ProductSnapshot;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.reporting.data.SalesReport;
import io.nextpos.shared.DummyObjects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@Transactional
class ReportingServiceImplTest {

    @Autowired
    private ReportingService reportingService;

    @Autowired
    private OrderService orderService;


    @Test
    void generateSalesReport() {

        final String clientId = ReportingServiceImplTest.class.getSimpleName();
        final Client client = DummyObjects.dummyClient();
        client.setId(clientId);

        final Order createdOrder = this.createOrder(clientId);

        final SalesReport salesReport = reportingService.generateSalesReport(client, null);

        assertThat(salesReport).isNotNull();
        assertThat(salesReport.getSalesTotal()).isGreaterThan(BigDecimal.ZERO);

        orderService.deleteOrder(createdOrder);
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