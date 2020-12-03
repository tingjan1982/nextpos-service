package io.nextpos.ordermanagement.web.factory;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.merchandising.data.ProductLevelOffer;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.web.model.OrderLineItemRequest;
import io.nextpos.ordermanagement.web.model.OrderProductOptionRequest;
import io.nextpos.ordermanagement.web.model.OrderRequest;
import io.nextpos.product.data.Product;
import io.nextpos.product.service.ProductService;
import io.nextpos.shared.DummyObjects;
import io.nextpos.tablelayout.data.TableLayout;
import io.nextpos.tablelayout.service.TableLayoutService;
import org.assertj.core.data.Index;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

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

    @Autowired
    private TableLayoutService tableLayoutService;

    private Client client;

    private TableLayout.TableDetails tableDetails;

    @BeforeEach
    void prepare() {
        client = DummyObjects.dummyClient();
        clientService.saveClient(client);

        final TableLayout tableLayout = DummyObjects.dummyTableLayout(client);
        tableLayoutService.saveTableLayout(tableLayout);

        tableDetails = tableLayout.getTables().get(0);
    }

    @Test
    void newOrder() {

        final Product product = new Product(client, DummyObjects.dummyProductVersion());
        productService.saveProduct(product);
        productService.deployProduct(product.getId());

        final OrderProductOptionRequest poRequest = new OrderProductOptionRequest("ice", "normal", new BigDecimal("10"));
        final OrderLineItemRequest line1 = new OrderLineItemRequest(product.getId(), 1, new BigDecimal("20"), List.of(poRequest), ProductLevelOffer.GlobalProductDiscount.NO_DISCOUNT, BigDecimal.ZERO);
        final OrderRequest request = new OrderRequest(Order.OrderType.IN_STORE, "", null, List.of(tableDetails.getId()), null, null, List.of(line1));

        final Order order = orderCreationFactory.newOrder(client, request);

        assertThat(order.getId()).isNotNull();
        assertThat(order.getTables()).isNotEmpty();
        assertThat(order.getTables()).allSatisfy(ti -> {
            assertThat(ti).isNotNull();
            assertThat(ti.getTableId()).isEqualTo(tableDetails.getId());
        });
        assertThat(order.getOrderLineItems()).hasSize(1);
        assertThat(order.getOrderLineItems()).satisfies(li -> {
            assertThat(li.getId()).isEqualTo(order.getId() + "-1");
            assertThat(li.getProductSnapshot().getPrice()).isZero();
            assertThat(li.getProductSnapshot().getOverridePrice()).isEqualTo("20");
            assertThat(li.getProductSnapshot().getProductPriceWithOptions()).isEqualTo("20");
            assertThat(li.getProductSnapshot().getProductOptions()).hasSize(1);

        }, Index.atIndex(0));
    }
}