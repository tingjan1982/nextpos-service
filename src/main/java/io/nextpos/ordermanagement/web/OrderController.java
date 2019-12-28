package io.nextpos.ordermanagement.web;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientObjectOwnershipService;
import io.nextpos.merchandising.data.OrderLevelOffer;
import io.nextpos.merchandising.service.MerchandisingService;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.ordermanagement.data.OrderStateChange;
import io.nextpos.ordermanagement.data.OrderStateChangeBean;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.ordermanagement.web.factory.OrderCreationFactory;
import io.nextpos.ordermanagement.web.model.*;
import io.nextpos.shared.web.ClientResolver;
import io.nextpos.shared.web.model.DeleteObjectResponse;
import io.nextpos.shared.web.model.SimpleObjectResponse;
import io.nextpos.shared.web.model.SimpleObjectsResponse;
import io.nextpos.tablelayout.data.TableLayout;
import io.nextpos.tablelayout.service.TableLayoutService;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    private final ClientObjectOwnershipService clientObjectOwnershipService;

    private final TableLayoutService tableLayoutService;

    private final OrderCreationFactory orderCreationFactory;

    private final MerchandisingService merchandisingService;

    @Autowired
    public OrderController(final OrderService orderService, final ClientObjectOwnershipService clientObjectOwnershipService, final TableLayoutService tableLayoutService, final OrderCreationFactory orderCreationFactory, final MerchandisingService merchandisingService) {
        this.orderService = orderService;
        this.clientObjectOwnershipService = clientObjectOwnershipService;
        this.tableLayoutService = tableLayoutService;
        this.orderCreationFactory = orderCreationFactory;
        this.merchandisingService = merchandisingService;
    }

    @PostMapping
    public OrderResponse createOrder(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client, @RequestBody OrderRequest orderRequest) {

        Order order = orderCreationFactory.newOrder(client, orderRequest);
        final Order createdOrder = orderService.createOrder(order);

        return toOrderResponse(createdOrder);
    }

    /**
     * The merge function is to circumvent the following potential error:
     * java.lang.IllegalStateException: Duplicate key A1 (attempted merging values OrdersResponse.LightOrderResponse(orderId=5d65f8587d6ffa3008fc2023, state=OPEN, total=TaxableAmount(taxRate=0.05, amountWithoutTax=155.00, amountWithTax=162.7500, tax=7.7500)) and OrdersResponse.LightOrderResponse(orderId=5d65f90a7d6ffa3008fc2024, state=OPEN, total=TaxableAmount(taxRate=0.05, amountWithoutTax=155.00, amountWithTax=162.7500, tax=7.7500)))
     * <p>
     * toMap reference:
     * https://www.geeksforgeeks.org/collectors-tomap-method-in-java-with-examples/
     */
    @GetMapping("/inflight")
    public OrdersResponse getOrders(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        List<Order> orders = orderService.getInflightOrders(client.getId());
        final Map<String, List<OrdersResponse.LightOrderResponse>> orderResponses = orders.stream()
                .filter(o -> o.getTableInfo() != null)
                .map(o -> {
                    final Optional<TableLayout.TableDetails> tableDetails = tableLayoutService.getTableDetails(o.getTableInfo().getTableId());
                    return Pair.of(o, tableDetails);
                })
                .filter(pair -> pair.getRight().isPresent())
                .map(pair -> {
                    final Order o = pair.getLeft();
                    final TableLayout.TableDetails table = pair.getRight().get();
                    final TableLayout tableLayout = table.getTableLayout();

                    return new OrdersResponse.LightOrderResponse(o.getId(),
                            tableLayout.getId(),
                            tableLayout.getLayoutName(),
                            table.getTableName(),
                            o.getCustomerCount(),
                            o.getCreatedDate(),
                            o.getState(),
                            o.getTotal());
                })
                .collect(Collectors.groupingBy(OrdersResponse.LightOrderResponse::getTableLayoutId, Collectors.toList()));

        return new OrdersResponse(orderResponses);
    }

    @GetMapping("/availableTables")
    public TablesResponse getTables(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        final List<String> occupiedTableIds = orderService.getInflightOrders(client.getId()).stream()
                .map(o -> o.getTableInfo().getTableId())
                .collect(Collectors.toList());

        final Map<String, List<SimpleObjectResponse>> availableTables = tableLayoutService.getTableLayouts(client).stream()
                .flatMap(tl -> tl.getTables().stream())
                .filter(t -> !occupiedTableIds.contains(t.getId()))
                .collect(Collectors.groupingBy(t -> t.getTableLayout().getLayoutName(),
                        Collectors.mapping(t -> new SimpleObjectResponse(t.getId(), t.getTableName()), Collectors.toList())));

        return new TablesResponse(availableTables);
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable String id) {

        final Order order = orderService.getOrder(id);
        return toOrderResponse(order);
    }

    @PostMapping("/{id}/applyDiscount")
    public OrderResponse applyOrderDiscount(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                            @PathVariable final String id,
                                            @Valid @RequestBody DiscountRequest discountRequest) {

        final Order order = clientObjectOwnershipService.checkWithClientIdOwnership(client, () -> orderService.getOrder(id));
        final OrderLevelOffer.GlobalOrderDiscount globalOrderDiscount = OrderLevelOffer.GlobalOrderDiscount.valueOf(discountRequest.getOrderDiscount());

        BigDecimal discount = discountRequest.getDiscount().divide(BigDecimal.valueOf(100));
        final Order updatedOrder = merchandisingService.applyGlobalOrderDiscount(order, globalOrderDiscount, discount);

        return toOrderResponse(updatedOrder);
    }

    @PostMapping("/{id}/copy")
    public OrderResponse copyOrder(@PathVariable final String id) {

        Order copiedOrder = orderService.copyOrder(id);
        return toOrderResponse(copiedOrder);
    }

    @DeleteMapping("/{id}")
    public DeleteObjectResponse deleteOrder(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client, @PathVariable final String id) {

        final Order order = clientObjectOwnershipService.checkWithClientIdOwnership(client, () -> orderService.getOrder(id));
        orderService.deleteOrder(order);

        return new DeleteObjectResponse(id);
    }


    @PostMapping("/{id}/lineitems")
    public OrderResponse AddOrderLineItem(@PathVariable String id, @RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client, @Valid @RequestBody OrderLineItemRequest orderLineItemRequest) {

        final Order order = orderService.getOrder(id);
        final OrderLineItem orderLineItem = orderCreationFactory.newOrderLineItem(client, orderLineItemRequest);

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
    public OrderResponse updateOrderLineItem(@PathVariable String id, @PathVariable String lineItemId, @Valid @RequestBody UpdateOrderLineItemRequest updateOrderLineItemRequest) {

        final Order order = orderService.updateOrderLineItem(id, lineItemId, updateOrderLineItemRequest.getQuantity());

        return toOrderResponse(order);
    }

    @PostMapping("/{id}/process")
    public OrderStateChangeResponse stateChange(@PathVariable String id, @RequestParam("action") Order.OrderAction orderAction) {

        OrderStateChangeBean orderStateChangeBean = orderService.performOrderAction(id, orderAction);

        return toOrderStateChangeResponse(orderStateChangeBean);
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

    private OrderResponse toOrderResponse(final Order order) {

        final List<OrderResponse.OrderLineItemResponse> orderLineItems = order.getOrderLineItems().stream()
                .map(li -> {
                    final String options = li.getProductSnapshot().getProductOptions().stream()
                            .map(po -> String.format("%s: %s (%s)", po.getOptionName(), po.getOptionValue(), po.getOptionPrice()))
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
                order.getSerialId(),
                order.getTableInfo(),
                order.getServedBy(),
                order.getCreatedDate(),
                order.getModifiedDate(),
                order.getState(),
                order.getTotal(),
                order.getDiscountedTotal(),
                order.getServiceCharge(),
                order.getOrderTotal(),
                order.getCurrency(),
                orderLineItems,
                order.getMetadata(),
                order.getDemographicData());
    }

}
