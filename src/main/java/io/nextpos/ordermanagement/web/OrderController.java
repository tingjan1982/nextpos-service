package io.nextpos.ordermanagement.web;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientSetting;
import io.nextpos.client.service.ClientSettingsService;
import io.nextpos.ordermanagement.data.*;
import io.nextpos.ordermanagement.event.OrderStateChangeEvent;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.ordermanagement.web.model.*;
import io.nextpos.product.data.Product;
import io.nextpos.product.data.ProductVersion;
import io.nextpos.product.service.ProductService;
import io.nextpos.product.web.model.SimpleObjectResponse;
import io.nextpos.settings.data.CountrySettings;
import io.nextpos.settings.service.SettingsService;
import io.nextpos.shared.exception.GeneralApplicationException;
import io.nextpos.shared.web.ClientResolver;
import io.nextpos.shared.web.model.SimpleObjectsResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

    private final ClientSettingsService clientSettingsService;

    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public OrderController(final OrderService orderService, final ProductService productService, final SettingsService settingsService, final ClientSettingsService clientSettingsService, final ApplicationEventPublisher eventPublisher) {
        this.orderService = orderService;
        this.productService = productService;
        this.settingsService = settingsService;
        this.clientSettingsService = clientSettingsService;
        this.eventPublisher = eventPublisher;
    }

    @PostMapping
    public OrderResponse createOrder(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client, @RequestBody OrderRequest orderRequest) {

        Order order = fromOrderRequest(client, orderRequest);
        final Order createdOrder = orderService.createOrder(order);

        return toOrderResponse(createdOrder);
    }

    /**
     * The merge function is to circumvent the following potential error:
     * java.lang.IllegalStateException: Duplicate key A1 (attempted merging values OrdersResponse.LightOrderResponse(orderId=5d65f8587d6ffa3008fc2023, state=OPEN, total=TaxableAmount(taxRate=0.05, amountWithoutTax=155.00, amountWithTax=162.7500, tax=7.7500)) and OrdersResponse.LightOrderResponse(orderId=5d65f90a7d6ffa3008fc2024, state=OPEN, total=TaxableAmount(taxRate=0.05, amountWithoutTax=155.00, amountWithTax=162.7500, tax=7.7500)))
     * <p>
     * toMap reference:
     * https://www.geeksforgeeks.org/collectors-tomap-method-in-java-with-examples/
     *
     * @param client
     * @return
     */
    @GetMapping("/inflight")
    public OrdersResponse getOrders(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        List<Order> orders = orderService.getInflightOrders(client.getId());
        final Map<String, OrdersResponse.LightOrderResponse> orderResponses = orders.stream()
                .collect(Collectors.toMap(Order::getTableId,
                        o -> new OrdersResponse.LightOrderResponse(o.getId(), o.getState(), o.getTotal()),
                        (o1, o2) -> {
                            LOGGER.warn("Found orders with same table, replacing with the latter one: o1={}, o2={}", o1.getOrderId(), o2.getOrderId());
                            return o2;
                        }));

        return new OrdersResponse(orderResponses);
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable String id) {

        final Order order = orderService.getOrder(id);
        return toOrderResponse(order);
    }

    @DeleteMapping("/{id}")
    public void deleteOrder(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client, @PathVariable final String id) {
        // todo: implement delete order.
    }


    @PostMapping("/{id}/lineitems")
    public OrderResponse AddOrderLineItem(@PathVariable String id, @RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client, @Valid @RequestBody OrderLineItemRequest orderLineItemRequest) {

        final Order order = orderService.getOrder(id);
        final OrderLineItem orderLineItem = fromOrderLineItemRequest(client, orderLineItemRequest);

        orderService.addOrderLineItem(order, orderLineItem);

        return toOrderResponse(order);
    }

    @PostMapping("/{id}/lineitems/deliver")
    public SimpleObjectsResponse deliverLineItems(@PathVariable final String id, @Valid @RequestBody UpdateLineItemsRequest updateLineItemsRequest) {

        final List<OrderLineItem> updatedOrderLineItems = orderService.deliverLineItems(id, updateLineItemsRequest.getLineItemIds());
        final List<SimpleObjectResponse> simpleObjects = updatedOrderLineItems.stream()
                .map(li -> new SimpleObjectResponse(li.getId(), li.getProductSnapshot().getName()))
                .collect(Collectors.toList());

        return new SimpleObjectsResponse(simpleObjects);
    }

    @PatchMapping("/{id}/lineitems/{lineItemId}")
    public OrderResponse updateOrderLineItem(@PathVariable String id, @PathVariable String lineItemId, @RequestBody UpdateOrderLineItemRequest updateOrderLineItemRequest) {

        final Order order = orderService.updateOrderLineItem(id, lineItemId, updateOrderLineItemRequest);

        return toOrderResponse(order);
    }

    @PostMapping("/{id}/process")
    public OrderStateChangeResponse stateChange(@PathVariable String id, @RequestParam("action") Order.OrderAction orderAction) {

        final Order order = orderService.getOrder(id);
        final CompletableFuture<OrderStateChangeBean> future = new CompletableFuture<>();
        eventPublisher.publishEvent(new OrderStateChangeEvent(this, order, orderAction, future));

        final OrderStateChangeBean orderStateChangeBean = this.getOrderStateChangeBeanFromFuture(future);

        return toOrderStateChangeResponse(orderStateChangeBean);
    }

    private OrderStateChangeBean getOrderStateChangeBeanFromFuture(CompletableFuture<OrderStateChangeBean> future) throws GeneralApplicationException {

        try {
            return future.get(15, TimeUnit.SECONDS);

        } catch (GeneralApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralApplicationException(e.getMessage());
        }
    }

    private OrderStateChangeResponse toOrderStateChangeResponse(final OrderStateChangeBean orderStateChangeBean) {

        final OrderStateChange orderStateChange = orderStateChangeBean.getOrderStateChange();
        final OrderStateChange.OrderStateChangeEntry orderStateChangeEntry = orderStateChange.getLastEntry();

        List<OrderStateChangeResponse.PrinterInstructionResponse> printerInstructions = List.of();

        if (orderStateChangeBean.getPrinterInstructions().isPresent()) {
            printerInstructions = orderStateChangeBean.getPrinterInstructions().get().getPrinterInstructions().values().stream()
                    .map(pi -> new OrderStateChangeResponse.PrinterInstructionResponse(pi.getPrinterIpAddresses(),
                            pi.getNoOfPrintCopies(),
                            pi.getPrintInstruction())).collect(Collectors.toList());
        }

        return new OrderStateChangeResponse(orderStateChange.getOrderId(),
                orderStateChangeEntry.getFromState(),
                orderStateChangeEntry.getToState(),
                orderStateChangeEntry.getTimestamp(),
                printerInstructions);
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

        client.getClientSettings(ClientSetting.SettingName.SERVICE_CHARGE).ifPresent(sc -> {
            if (sc.isEnabled()) {
                final BigDecimal serviceCharge = clientSettingsService.getActualStoredValue(sc, BigDecimal.class);
                order.setServiceCharge(serviceCharge);
            }
        });

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

        if (product.getProductLabel() != null) {
            productSnapshot.setLabelInformation(product.getProductLabel().getId(), product.getProductLabel().getName());
        }

        final OrderLineItem orderLineItem = new OrderLineItem(productSnapshot, li.getQuantity(), countrySettings.getTaxRate());

        if (product.getWorkingArea() != null) {
            orderLineItem.setWorkingAreaId(product.getWorkingArea().getId());
        }

        return orderLineItem;
    }


    private OrderResponse toOrderResponse(final Order order) {

        final List<OrderResponse.OrderLineItemResponse> orderLineItems = order.getOrderLineItems().stream()
                .map(li -> {
                    final String options = li.getProductSnapshot().getProductOptions().stream()
                            .map(po -> String.format("%s: %s => %s", po.getOptionName(), po.getOptionValue(), po.getOptionPrice()))
                            .collect(Collectors.joining(", "));

                    return new OrderResponse.OrderLineItemResponse(li.getId(),
                            li.getProductSnapshot().getName(),
                            li.getProductSnapshot().getPrice(),
                            li.getQuantity(),
                            li.getSubTotal(),
                            li.getState(),
                            options);

                }).collect(Collectors.toList());

        return new OrderResponse(order.getId(),
                order.getTableId(),
                order.getCreatedDate(),
                order.getModifiedDate(),
                order.getState(),
                order.getTotal(),
                order.getServiceCharge(),
                order.getOrderTotal(),
                order.getCurrency(),
                orderLineItems);
    }

}
