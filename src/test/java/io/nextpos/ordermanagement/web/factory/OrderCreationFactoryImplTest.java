package io.nextpos.ordermanagement.web.factory;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.web.model.OrderLineItemRequest;
import io.nextpos.ordermanagement.web.model.OrderProductOptionRequest;
import io.nextpos.ordermanagement.web.model.OrderRequest;
import io.nextpos.product.data.Product;
import io.nextpos.product.service.ProductService;
import io.nextpos.shared.DummyObjects;
import org.assertj.core.data.Index;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class OrderCreationFactoryImplTest {

    @Autowired
    private OrderCreationFactory orderCreationFactory;

    @Autowired
    private ClientService clientService;

    @Autowired
    private ProductService productService;

    private Client client;

    @BeforeEach
    void prepare() {
        client = DummyObjects.dummyClient();
        clientService.saveClient(client);
    }

    @Test
    void newOrder() {

        final Product product = new Product(client, DummyObjects.dummyProductVersion());
        productService.saveProduct(product);
        productService.deployProduct(product.getId());

        final OrderProductOptionRequest poRequest = new OrderProductOptionRequest("ice", "normal", BigDecimal.ZERO);
        final OrderLineItemRequest line1 = new OrderLineItemRequest(product.getId(), 1, List.of(poRequest));
        final OrderRequest request = new OrderRequest("A1", List.of(line1));

        final Order order = orderCreationFactory.newOrder(client, request);

        assertThat(order.getId()).isNotNull();
        assertThat(order.getSerialId()).isNotNull();
        assertThat(order.getTableId()).isEqualTo("A1");
        assertThat(order.getOrderLineItems()).hasSize(1);
        assertThat(order.getOrderLineItems()).satisfies(li -> {
            assertThat(li.getId()).isEqualTo(order.getId() + "-1");
            assertThat(li.getProductSnapshot()).isNotNull();
            assertThat(li.getProductSnapshot().getProductOptions()).hasSize(1);

        }, Index.atIndex(0));
    }
}