package io.nextpos.ordermanagement.web;

import io.nextpos.client.data.Client;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.ordermanagement.data.OrderStateChange;
import io.nextpos.ordermanagement.data.ProductSnapshot;
import io.nextpos.ordermanagement.event.OrderStateChangeEvent;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.ordermanagement.web.model.*;
import io.nextpos.product.data.Product;
import io.nextpos.product.data.ProductVersion;
import io.nextpos.product.service.ProductService;
import io.nextpos.settings.data.CountrySettings;
import io.nextpos.settings.service.SettingsService;
import io.nextpos.shared.exception.GeneralApplicationException;
import io.nextpos.shared.web.ClientResolver;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

// todo: major revisit to simplify the object transformation part.
@RestController
@RequestMapping("/orders")
public class OrderController {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    private final ProductService productService;

    private final SettingsService settingsService;

    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public OrderController(final OrderService orderService, final ProductService productService, final SettingsService settingsService, final ApplicationEventPublisher eventPublisher) {
        this.orderService = orderService;
        this.productService = productService;
        this.settingsService = settingsService;
        this.eventPublisher = eventPublisher;
    }

    @PostMapping
    public OrderResponse createOrder(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client, @RequestBody OrderRequest orderRequest) {

        Order order = fromOrderRequest(client, orderRequest);
        final Order createdOrder = orderService.createOrder(order);

        return toOrderResponse(createdOrder);
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable String id) {

        final Order order = orderService.getOrder(id);
        return toOrderResponse(order);
    }

    @PostMapping("/{id}/lineitems")
    public OrderResponse AddOrderLineItem(@PathVariable String id, @RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client, @Valid @RequestBody OrderLineItemRequest orderLineItemRequest) {

        final Order order = orderService.getOrder(id);
        final OrderLineItem orderLineItem = fromOrderLineItemRequest(client, orderLineItemRequest);

        orderService.addOrderLineItem(order, orderLineItem);

        return toOrderResponse(order);
    }

    @PatchMapping("/{id}/lineitems/{lineItemId}")
    public OrderResponse updateOrderLineItem(@PathVariable String id, @PathVariable String lineItemId, @RequestBody UpdateOrderLineItemRequest updateOrderLineItemRequest) {

        final Order order = orderService.updateOrderLineItem(id, lineItemId, updateOrderLineItemRequest);

        return toOrderResponse(order);
    }

    @PostMapping("/{id}/process")
    public OrderStateChangeResponse stateChange(@PathVariable String id, @RequestParam("action") Order.OrderAction orderAction) {

        final Order order = orderService.getOrder(id);
        final CompletableFuture<OrderStateChange> future = new CompletableFuture<>();
        eventPublisher.publishEvent(new OrderStateChangeEvent(this, order, orderAction, future));

        try {
            final OrderStateChange orderStateChange = future.get(30, TimeUnit.SECONDS);
            return toOrderStateChangeResponse(orderStateChange);

        } catch (GeneralApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralApplicationException(e.getMessage());
        }
    }

    private OrderStateChangeResponse toOrderStateChangeResponse(final OrderStateChange orderStateChange) {

        final OrderStateChange.OrderStateChangeEntry orderStateChangeEntry = orderStateChange.getLastEntry();

        return new OrderStateChangeResponse(orderStateChange.getOrderId(),
                orderStateChangeEntry.getFromState(),
                orderStateChangeEntry.getToState(),
                orderStateChangeEntry.getTimestamp());
    }

    private Order fromOrderRequest(final Client client, final OrderRequest orderRequest) {

        final CountrySettings countrySettings = settingsService.getCountrySettings(client.getCountryCode());
        final Order order = new Order(client.getId(), countrySettings.getTaxRate(), countrySettings.getCurrency());

        if (StringUtils.isNotEmpty(orderRequest.getTableId())) {
            order.setTableId(orderRequest.getTableId());
        }

        if (!CollectionUtils.isEmpty(orderRequest.getLineItems())) {
            orderRequest.getLineItems().forEach(li -> {
                final OrderLineItem orderLineItem = fromOrderLineItemRequest(client, li);
                order.addOrderLineItem(orderLineItem);
            });

        }

        LOGGER.info("Transformed to order object: {}", order);

        return order;
    }

    private OrderLineItem fromOrderLineItemRequest(final Client client, final OrderLineItemRequest li) {

        final CountrySettings countrySettings = settingsService.getCountrySettings(client.getCountryCode());

        final Product product = productService.getProduct(li.getProductId());
        final ProductVersion productVersion = product.getLiveVersion();
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

        return new OrderLineItem(productSnapshot, li.getQuantity(), countrySettings.getTaxRate());
    }


    private OrderResponse toOrderResponse(final Order order) {

        final List<OrderResponse.OrderLineItemResponse> orderLineItems = order.getOrderLineItems().stream()
                .map(li -> {
                    final String options = li.getProductSnapshot().getProductOptions().stream()
                            .map(po -> String.format("%s: %s => %s", po.getOptionName(), po.getOptionValue(), po.getOptionPrice()))
                            .collect(Collectors.joining(", "));

                    return new OrderResponse.OrderLineItemResponse(li.getId(), li.getProductSnapshot().getName(), li.getProductSnapshot().getPrice(), li.getQuantity(), li.getSubTotal(), options);
                }).collect(Collectors.toList());

        return new OrderResponse(order.getId(),
                order.getTableId(),
                order.getCreatedDate(),
                order.getModifiedDate(),
                order.getState(),
                order.getTotal(),
                order.getCurrency(),
                orderLineItems);
    }

}
