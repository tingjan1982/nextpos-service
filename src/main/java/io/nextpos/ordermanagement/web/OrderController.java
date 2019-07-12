package io.nextpos.ordermanagement.web;

import io.nextpos.client.data.Client;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.ordermanagement.data.ProductSnapshot;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.ordermanagement.web.model.OrderRequest;
import io.nextpos.ordermanagement.web.model.OrderResponse;
import io.nextpos.product.data.Product;
import io.nextpos.product.data.ProductVersion;
import io.nextpos.product.service.ProductService;
import io.nextpos.settings.data.CountrySettings;
import io.nextpos.settings.service.SettingsService;
import io.nextpos.shared.web.ClientResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

// todo: major revisit to simplify the object transformation part.
@RestController
@RequestMapping("/orders")
public class OrderController {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    private final ProductService productService;

    private final SettingsService settingsService;

    @Autowired
    public OrderController(final OrderService orderService, final ProductService productService, final SettingsService settingsService) {
        this.orderService = orderService;
        this.productService = productService;
        this.settingsService = settingsService;
    }

    @PostMapping
    public OrderResponse createOrder(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client, @RequestBody OrderRequest orderRequest) {

        Order order = fromOrderRequest(client, orderRequest);
        final Order createdOrder = orderService.createOrder(order);

        return toOrderResponse(createdOrder);
    }

    private Order fromOrderRequest(final Client client, final OrderRequest orderRequest) {

        final CountrySettings countrySettings = settingsService.getCountrySettings("TW");
        final Order order = new Order(client.getId(), countrySettings.getTaxRate());

        if (!CollectionUtils.isEmpty(orderRequest.getLineItems())) {
            orderRequest.getLineItems().forEach(li -> {

                final Product product = productService.getProduct(li.getProductId());
                final ProductVersion productVersion = product.getLatestVersion();
                List<ProductSnapshot.ProductOptionSnapshot> productOptionSnapshots = Collections.emptyList();

                if (!CollectionUtils.isEmpty(li.getProductOptions())) {
                    productOptionSnapshots = li.getProductOptions().stream()
                            .map(po -> new ProductSnapshot.ProductOptionSnapshot(po.getOptionName(), po.getOptionValue(), po.getOptionPrice()))
                            .collect(Collectors.toList());

                }

                final ProductSnapshot productSnapshot = new ProductSnapshot(product.getId(),
                        productVersion.getProductName(),
                        productVersion.getSku(),
                        productVersion.getPrice(),
                        productOptionSnapshots);
                final OrderLineItem orderLineItem = new OrderLineItem(productSnapshot, li.getQuantity(), countrySettings.getTaxRate());
                order.addOrderLineItem(orderLineItem);
            });

        }

        return order;
    }

    private OrderResponse toOrderResponse(final Order order) {

        return new OrderResponse(order.getId(), order.getState(), order.getTotal());
    }

}
