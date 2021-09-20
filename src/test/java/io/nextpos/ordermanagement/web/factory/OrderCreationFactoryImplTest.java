package io.nextpos.ordermanagement.web.factory;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.merchandising.data.ProductLevelOffer;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.ordermanagement.data.OrderSettings;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.ordermanagement.web.model.ComboOrderLineItemRequest;
import io.nextpos.ordermanagement.web.model.OrderLineItemRequest;
import io.nextpos.ordermanagement.web.model.OrderProductOptionRequest;
import io.nextpos.ordermanagement.web.model.OrderRequest;
import io.nextpos.product.data.Product;
import io.nextpos.product.service.ProductService;
import io.nextpos.settings.data.CountrySettings;
import io.nextpos.shared.DummyObjects;
import io.nextpos.tablelayout.data.TableLayout;
import io.nextpos.tablelayout.service.TableLayoutService;
import io.nextpos.workingarea.data.WorkingArea;
import io.nextpos.workingarea.service.WorkingAreaService;
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
    private OrderService orderService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private ProductService productService;

    @Autowired
    private WorkingAreaService workingAreaService;

    @Autowired
    private TableLayoutService tableLayoutService;

    @Autowired
    private CountrySettings countrySettings;

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
        final OrderLineItemRequest line1 = new OrderLineItemRequest(product.getId(), 1, "sku", new BigDecimal("20"), List.of(poRequest), ProductLevelOffer.GlobalProductDiscount.NO_DISCOUNT, BigDecimal.ZERO);
        final OrderRequest request = new OrderRequest(Order.OrderType.IN_STORE, List.of(tableDetails.getId()), null, null, List.of(line1));

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

    @Test
    void newComboLineItem() {

        final WorkingArea potArea = new WorkingArea(client, "Pot Area");
        workingAreaService.saveWorkingArea(potArea);
        final WorkingArea meatArea = new WorkingArea(client, "Meat area");
        workingAreaService.saveWorkingArea(meatArea);

        final Product combo = Product.builder(client).productNameAndPrice("Combo", new BigDecimal("100")).build();
        productService.saveProduct(combo);
        final Product potFlavor = Product.builder(client).productNameAndPrice("Pot flavor", new BigDecimal("50")).build();
        potFlavor.setWorkingArea(potArea);
        productService.saveProduct(potFlavor);
        final Product meat = Product.builder(client).productNameAndPrice("Meat", new BigDecimal("399")).build();
        meat.setWorkingArea(meatArea);
        productService.saveProduct(meat);

        ComboOrderLineItemRequest comboRequest = new ComboOrderLineItemRequest();
        comboRequest.setProductId(combo.getId());
        comboRequest.setQuantity(1);
        comboRequest.getChildLineItems().add(toRequest(potFlavor));
        comboRequest.getChildLineItems().add(toRequest(meat));

        final OrderLineItem comboLineItem = orderCreationFactory.newOrderLineItem(client, comboRequest);

        assertThat(comboLineItem.getProductSnapshot().getId()).isEqualTo(combo.getId());
        assertThat(comboLineItem.getComboTotal()).isEqualTo("549");
        assertThat(comboLineItem.getWorkingAreaId()).isNull();
        assertThat(comboLineItem.getChildLineItems()).hasSize(2);
        assertThat(comboLineItem.getChildLineItems()).allSatisfy(li -> {
            assertThat(li.getLineItemSubTotal()).isNotZero();
            assertThat(li.getWorkingAreaId()).isNotNull();
        });

        final OrderSettings orderSettings = DummyObjects.orderSettings(countrySettings);
        final Order order = new Order(client.getId(), orderSettings);
        orderService.createOrder(order);

        orderService.addOrderLineItem(client, order, comboLineItem);

        assertThat(order.getOrderLineItems()).hasSize(3);
        assertThat(order.getOrderTotal()).isEqualTo("549");
        assertThat(order.getOrderLineItems().get(1).getWorkingAreaId()).isEqualTo(potArea.getId());
        assertThat(order.getOrderLineItems().get(2).getWorkingAreaId()).isEqualTo(meatArea.getId());

        order.getOrderLineItems().forEach(li -> {
            if (li.getChildLineItems().isEmpty()) {
                assertThat(li.getAssociatedLineItemId()).isEqualTo(comboLineItem.getId());
            }
        });

        orderService.deleteOrderLineItem(order, comboLineItem.getId());

        assertThat(order.getOrderLineItems()).isEmpty();
        assertThat(order.getDeletedOrderLineItems()).hasSize(3);
        assertThat(order.getOrderTotal()).isZero();
    }

    private OrderLineItemRequest toRequest(Product product) {
        final OrderLineItemRequest request = new OrderLineItemRequest();
        request.setProductId(product.getId());
        request.setQuantity(1);

        return request;
    }
}