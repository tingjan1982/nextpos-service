package io.nextpos.ordermanagement.web;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientObjectOwnershipService;
import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.datetime.service.ZonedDateRangeBuilder;
import io.nextpos.membership.service.MembershipService;
import io.nextpos.membership.web.model.MembershipResponse;
import io.nextpos.merchandising.data.Offer;
import io.nextpos.merchandising.data.ProductLevelOffer;
import io.nextpos.merchandising.service.MerchandisingService;
import io.nextpos.ordermanagement.data.*;
import io.nextpos.ordermanagement.service.OrderMessagingService;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.ordermanagement.service.ShiftService;
import io.nextpos.ordermanagement.service.bean.UpdateLineItem;
import io.nextpos.ordermanagement.web.factory.OrderCreationFactory;
import io.nextpos.ordermanagement.web.model.*;
import io.nextpos.ordertransaction.service.OrderTransactionService;
import io.nextpos.ordertransaction.web.model.OrderTransactionResponse;
import io.nextpos.reporting.data.DateParameterType;
import io.nextpos.shared.aspect.OrderLogAction;
import io.nextpos.shared.aspect.OrderLogParam;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.web.ClientResolver;
import io.nextpos.tablelayout.service.TableLayoutService;
import io.nextpos.tablelayout.web.model.TableDetailsResponse;
import io.nextpos.workingarea.data.Printer;
import io.nextpos.workingarea.data.PrinterInstructions;
import io.nextpos.workingarea.data.SinglePrintInstruction;
import io.nextpos.workingarea.service.PrinterInstructionService;
import io.nextpos.workingarea.service.WorkingAreaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    private final OrderMessagingService orderMessagingService;

    private final OrderTransactionService orderTransactionService;

    private final ClientObjectOwnershipService clientObjectOwnershipService;

    private final TableLayoutService tableLayoutService;

    private final OrderCreationFactory orderCreationFactory;

    private final MerchandisingService merchandisingService;

    private final ShiftService shiftService;

    private final PrinterInstructionService printerInstructionService;

    private final WorkingAreaService workingAreaService;

    private final MembershipService membershipService;

    @Autowired
    public OrderController(final OrderService orderService, OrderMessagingService orderMessagingService, final OrderTransactionService orderTransactionService, final ClientObjectOwnershipService clientObjectOwnershipService, final TableLayoutService tableLayoutService, final OrderCreationFactory orderCreationFactory, final MerchandisingService merchandisingService, final ShiftService shiftService, PrinterInstructionService printerInstructionService, WorkingAreaService workingAreaService, MembershipService membershipService) {
        this.orderService = orderService;
        this.orderMessagingService = orderMessagingService;
        this.orderTransactionService = orderTransactionService;
        this.clientObjectOwnershipService = clientObjectOwnershipService;
        this.tableLayoutService = tableLayoutService;
        this.orderCreationFactory = orderCreationFactory;
        this.merchandisingService = merchandisingService;
        this.shiftService = shiftService;
        this.printerInstructionService = printerInstructionService;
        this.workingAreaService = workingAreaService;
        this.membershipService = membershipService;
    }

    @PostMapping
    public OrderResponse createOrder(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                     @RequestBody OrderRequest orderRequest) {

        Order order = orderCreationFactory.newOrder(client, orderRequest);

        membershipService.updateMembership(orderRequest.getMembershipId(), order::updateMembership);

        final Order createdOrder = orderService.createOrder(order);

        return OrderResponse.toOrderResponse(createdOrder);
    }

    @GetMapping
    public OrdersByRangeResponse getOrdersByDateRange(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                      @RequestParam(name = "dateRange", required = false, defaultValue = "SHIFT") DateParameterType dateParameterType,
                                                      @RequestParam(name = "shiftId", required = false) String shiftId,
                                                      @RequestParam(name = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
                                                      @RequestParam(name = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
                                                      @RequestParam(name = "table", required = false) String table) {

        final ZonedDateRange zonedDateRange = resolveDateRange(client, dateParameterType, shiftId, fromDate, toDate);
        final OrderCriteria orderCriteria = OrderCriteria.instance().tableName(table);
        List<Order> orders = orderService.getOrders(client, zonedDateRange, orderCriteria);

        return new OrdersByRangeResponse(zonedDateRange, orders);
    }

    private ZonedDateRange resolveDateRange(Client client, DateParameterType dateParameterType, final String shiftId, final LocalDateTime fromDateParam, final LocalDateTime toDateParam) {

        final ZonedDateRangeBuilder builder = ZonedDateRangeBuilder.builder(client, dateParameterType);
        builder.dateRange(fromDateParam, toDateParam);

        if (shiftId != null) {
            Shift shift = shiftService.getShift(shiftId);
            builder.shift(shift);
        } else {
            shiftService.getMostRecentShift(client.getId()).ifPresent(builder::shift);
        }

        return builder.build();
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

        return toOrdersResponse(orders);
    }

    private OrdersResponse toOrdersResponse(final List<Order> orders) {
        return new OrdersResponse(orders);
    }

    @PostMapping("/markAllAsPrepared")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllAsPrepared(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        orderService.markAllLineItemsAsPrepared(client.getId());

        orderMessagingService.sendOrderLineItems(client.getId(), true);
        orderMessagingService.sendOrders(client.getId(), true);
    }

    @GetMapping("/availableTables")
    public TablesResponse getTables(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        final List<String> occupiedTableIds = orderService.getInflightOrders(client.getId()).stream()
                .filter(o -> !o.isTablesEmpty())
                .map(o -> o.getTables().stream().map(Order.TableInfo::getTableId).collect(Collectors.toList()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        final Map<String, List<TableDetailsResponse>> availableTables = tableLayoutService.getTableLayouts(client).stream()
                .flatMap(tl -> tl.getTables().stream())
                .filter(t -> !occupiedTableIds.contains(t.getId()))
                .collect(Collectors.groupingBy(t -> t.getTableLayout().getId(),
                        Collectors.mapping(TableDetailsResponse::fromTableDetails, Collectors.toList())));

        return new TablesResponse(availableTables);
    }

    @GetMapping("/search")
    public OrderResponse getOrderByInvoiceNumber(@RequestParam("invoiceNumber") String invNumber) {

        final Order order = orderTransactionService.getOrderByInvoiceNumber(invNumber);
        return OrderResponse.toOrderResponse(order);
    }

    @PostMapping("/orderOrdering")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void orderOrdering(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                              @Valid @RequestBody OrderingRequest request) {

        orderService.reorder(request.getOrderIds());

        orderMessagingService.sendOrders(client.getId(), false);
    }

    @PostMapping("/lineItemOrdering")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void lineItemOrdering(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                 @Valid @RequestBody LineItemOrderingRequest request) {

        orderService.orderLineItems(request.getLineItemOrderings());

        orderMessagingService.sendOrderLineItems(client.getId(), false);
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable String id) {

        final Order order = orderService.getOrder(id);
        final OrderResponse orderResponse = OrderResponse.toOrderResponse(order);

        orderService.getOrderStateChangeByOrderId(id).flatMap(OrderStateChange::getOrderPreparationDuration).ifPresent(orderResponse::setOrderPreparationTime);

        final List<OrderTransactionResponse> transactions = orderTransactionService.getOrderTransactionByOrderId(order.getId()).stream()
                .map(OrderTransactionResponse::toOrderTransactionResponse)
                .collect(Collectors.toList());

        orderResponse.setTransactions(transactions);

        if (order.getMembership() != null) {
            orderResponse.setMembership(new MembershipResponse(order.getMembership()));
        }

        return orderResponse;
    }

    @PostMapping("/{id}")
    @OrderLogAction
    public OrderResponse updateOrder(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                     @PathVariable final String id,
                                     @Valid @RequestBody OrderRequest orderRequest) {

        final Order order = clientObjectOwnershipService.checkWithClientIdOwnership(client, () -> orderService.getOrder(id));

        final UpdateTableInfo updateTableInfo = orderCreationFactory.updateTableInfoAndDemographicData(order, orderRequest);
        membershipService.updateMembership(orderRequest.getMembershipId(), order::updateMembership);

        final OrderResponse orderResponse = OrderResponse.toOrderResponse(orderService.saveOrder(order));

        if (updateTableInfo.hasChange()) {
            final SinglePrintInstruction updateTableInfoInstruction = printerInstructionService.createUpdateTableInfoInstruction(client, updateTableInfo);
            orderResponse.setUpdateTable(updateTableInfoInstruction);
        }

        return orderResponse;
    }


    /**
     * Move order {id} and all its line items from current table to specified table id(s).
     */
    @PostMapping("/{id}/move")
    @OrderLogAction
    public OrderResponse moveOrder(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                   @PathVariable final String id,
                                   @Valid @RequestBody MoveOrderRequest request) {

        final Order updatedOrder = orderService.moveOrder(id, request.getTargetOrderId());
        return OrderResponse.toOrderResponse(updatedOrder);
    }

    @PostMapping("/{id}/membership")
    @OrderLogAction
    public OrderResponse updateMembership(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                          @PathVariable final String id,
                                          @RequestBody OrderMembershipRequest request) {

        final Order order = clientObjectOwnershipService.checkWithClientIdOwnership(client, () -> orderService.getOrder(id));

        membershipService.updateMembership(request.getMembershipId(), order::updateMembership);

        return OrderResponse.toOrderResponse(orderService.saveOrder(order));
    }

    @PostMapping("/{id}/applyDiscount")
    @OrderLogAction
    public OrderResponse applyOrderDiscount(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                            @PathVariable final String id,
                                            @Valid @RequestBody DiscountRequest discountRequest) {

        final Order order = clientObjectOwnershipService.checkWithClientIdOwnership(client, () -> orderService.getOrder(id));

        final Order updatedOrder = merchandisingService.applyOrderOffer(order, discountRequest.getOfferId(), discountRequest.getDiscount());

        return OrderResponse.toOrderResponse(updatedOrder);
    }

    @PostMapping("/{id}/removeDiscount")
    @OrderLogAction
    public OrderResponse removeOrderDiscount(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                             @PathVariable final String id) {

        final Order order = clientObjectOwnershipService.checkWithClientIdOwnership(client, () -> orderService.getOrder(id));

        final Order updatedOrder = merchandisingService.removeOrderOffer(order);

        return OrderResponse.toOrderResponse(updatedOrder);
    }

    @PostMapping("/{id}/waiveServiceCharge")
    @OrderLogAction
    public OrderResponse waiveServiceCharge(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                            @PathVariable final String id,
                                            @OrderLogParam @RequestParam(value = "apply", defaultValue = "true") boolean apply) {

        final Order order = clientObjectOwnershipService.checkWithClientIdOwnership(client, () -> orderService.getOrder(id));

        final Order updatedOrder = merchandisingService.updateServiceCharge(order, apply);

        return OrderResponse.toOrderResponse(updatedOrder);
    }

    @PostMapping("/{id}/resetOrderOffers")
    public OrderResponse resetOrderOffers(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                          @PathVariable final String id) {

        final Order order = clientObjectOwnershipService.checkWithClientIdOwnership(client, () -> orderService.getOrder(id));

        final Order updatedOrder = merchandisingService.resetOrderOffers(order);

        return OrderResponse.toOrderResponse(updatedOrder);
    }

    @PostMapping("/{id}/copy")
    @OrderLogAction
    public OrderResponse copyOrder(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                   @PathVariable final String id) {

        Order copiedOrder = orderService.copyOrder(id);
        return OrderResponse.toOrderResponse(copiedOrder);
    }

    @PostMapping(value = "/{id}/orderToWorkingArea")
    public List<PrinterInstructionResponse> printOrderToWorkingArea(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                                    @PathVariable final String id,
                                                                    @RequestBody UpdateLineItemsRequest request) {

        final Order order = clientObjectOwnershipService.checkWithClientIdOwnership(client, () -> orderService.getOrder(id));

        final PrinterInstructions orderToWorkingArea = printerInstructionService.createOrderToWorkingArea(order, request.getLineItemIds(), true);

        return toPrinterInstructionResponses(orderToWorkingArea);
    }

    @GetMapping("/{id}/orderDetails")
    public PrinterInstructionResponse printOrderDetails(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                        @PathVariable final String id) {

        final Order order = clientObjectOwnershipService.checkWithClientIdOwnership(client, () -> orderService.getOrder(id));

        final String printInstruction = printerInstructionService.createOrderDetailsPrintInstruction(client, order, null);
        final List<String> printerIps = workingAreaService.getPrintersByServiceType(client, Printer.ServiceType.CHECKOUT).stream()
                .map(Printer::getIpAddress)
                .collect(Collectors.toList());

        return new PrinterInstructionResponse(printInstruction, printerIps, 1);
    }

    @DeleteMapping("/{id}")
    @OrderLogAction
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOrder(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                            @PathVariable final String id) {

        orderService.markOrderAsDeleted(id, true);
    }


    @PostMapping("/{id}/lineitems")
    @OrderLogAction
    public OrderResponse addOrderLineItem(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                          @PathVariable String id,
                                          @Valid @RequestBody OrderLineItemRequest request) {

        final OrderLineItem orderLineItem = orderCreationFactory.newOrderLineItem(client, request);

        if (request.getProductDiscount() != null) {
            BigDecimal discountValue = resolveProductDiscountValue(request.getProductDiscount(), request.getDiscountValue());
            merchandisingService.applyGlobalProductDiscount(orderLineItem, request.getProductDiscount(), discountValue);
        }

        final Order order = clientObjectOwnershipService.checkWithClientIdOwnership(client, () -> orderService.getOrder(id));
        orderService.addOrderLineItem(client, order, orderLineItem);

        return OrderResponse.toOrderResponse(order);
    }

    @PostMapping("/{id}/comboLineitems")
    @OrderLogAction
    public OrderResponse addComboOrderLineItem(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                               @PathVariable String id,
                                               @Valid @RequestBody ComboOrderLineItemRequest request) {

        final OrderLineItem comboOrderLineItem = orderCreationFactory.newOrderLineItem(client, request);

        final Order order = clientObjectOwnershipService.checkWithClientIdOwnership(client, () -> orderService.getOrder(id));
        orderService.addOrderLineItem(client, order, comboOrderLineItem);

        return OrderResponse.toOrderResponse(order);
    }

    @PostMapping("/{id}/lineitems/prepare")
    @OrderLogAction
    public OrderResponse prepareLineItems(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                          @PathVariable final String id,
                                          @Valid @RequestBody UpdateLineItemsRequest updateLineItemsRequest) {

        final Order updatedOrder = orderService.prepareLineItems(id, updateLineItemsRequest.getLineItemIds());

        return OrderResponse.toOrderResponse(updatedOrder);
    }

    @PostMapping("/{id}/lineitems/deliver")
    @OrderLogAction
    public OrderResponse deliverLineItems(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                          @PathVariable final String id,
                                          @Valid @RequestBody UpdateLineItemsRequest updateLineItemsRequest) {

        final Order updatedOrder = orderService.deliverLineItems(id, updateLineItemsRequest.getLineItemIds());

        return OrderResponse.toOrderResponse(updatedOrder);
    }

    @PostMapping("/{id}/lineitems/move")
    @OrderLogAction
    public OrderResponse moveLineItems(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                       @PathVariable final String id,
                                       @Valid @RequestBody MoveLineItemsRequest request) {

        final Order fromOrder = clientObjectOwnershipService.checkWithClientIdOwnership(client, () -> orderService.getOrder(id));

        final Order toOrder = orderService.getOrderByTableId(client, request.getTableId()).orElseGet(() -> {
            OrderRequest orderRequest = new OrderRequest();
            orderRequest.setOrderType(Order.OrderType.IN_STORE);
            orderRequest.setTableIds(List.of(request.getTableId()));

            final Order newOrder = orderCreationFactory.newOrder(client, orderRequest);
            return orderService.saveOrder(newOrder);
        });

        final Order updatedFromOrder = orderService.moveLineItems(fromOrder, toOrder, request.getLineItemIds());

        return OrderResponse.toOrderResponse(updatedFromOrder);
    }

    @PatchMapping("/{id}/lineitems/{lineItemId}")
    @OrderLogAction
    public OrderResponse updateOrderLineItem(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                             @PathVariable String id,
                                             @PathVariable String lineItemId,
                                             @Valid @RequestBody UpdateOrderLineItemRequest request) {

        request.setLineItemId(lineItemId);
        final Order order = clientObjectOwnershipService.checkWithClientIdOwnership(client, () -> orderService.getOrder(id));

        final ProductLevelOffer.GlobalProductDiscount productDiscount = request.getProductDiscount();
        BigDecimal discountValue = request.getDiscountValue();

        if (productDiscount != null) {
            discountValue = resolveProductDiscountValue(productDiscount, discountValue);
        }

        final UpdateLineItem updateLineItem = new UpdateLineItem(lineItemId,
                request.getQuantity(),
                request.getSku(),
                request.getOverridePrice(),
                request.toProductOptionSnapshots(),
                productDiscount,
                discountValue);

        final Order updatedOrder = orderService.updateOrderLineItem(order, updateLineItem);

        return OrderResponse.toOrderResponse(updatedOrder);
    }

    private BigDecimal resolveProductDiscountValue(ProductLevelOffer.GlobalProductDiscount productDiscount, BigDecimal discountValue) {

        if (productDiscount.getDiscountType() == Offer.DiscountType.PERCENT_OFF) {
            return discountValue.divide(BigDecimal.valueOf(100), 2, RoundingMode.CEILING);
        }

        return discountValue;
    }

    @PatchMapping("/{id}/lineitems/{lineItemId}/price")
    @OrderLogAction
    public OrderResponse updateOrderLineItemPrice(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                  @PathVariable String id,
                                                  @PathVariable String lineItemId,
                                                  @RequestParam("free") boolean free) {

        final Order order = clientObjectOwnershipService.checkWithClientIdOwnership(client, () -> orderService.getOrder(id));

        final BigDecimal price = free ? BigDecimal.ZERO : null;
        final Order updatedOrder = orderService.updateOrderLineItemPrice(order, lineItemId, price);

        return OrderResponse.toOrderResponse(updatedOrder);
    }

    @DeleteMapping("/{id}/lineitems/{lineItemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @OrderLogAction
    public void deleteOrderLineItem(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                    @PathVariable String id,
                                    @PathVariable String lineItemId) {

        final Order order = clientObjectOwnershipService.checkWithClientIdOwnership(client, () -> orderService.getOrder(id));
        orderService.deleteOrderLineItem(order, lineItemId);
    }

    @PostMapping("/{id}/quickCheckout")
    @OrderLogAction
    public OrderStateChangeResponse quickCheckout(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                  @PathVariable String id,
                                                  @RequestParam(name = "print", defaultValue = "true") boolean print) {

        OrderStateChangeBean inProcessStateChange = orderService.performOrderAction(id, Order.OrderAction.SUBMIT);

        if (!print) {
            inProcessStateChange.setPrinterInstructions(null);
        }

        return toOrderStateChangeResponse(inProcessStateChange);
    }

    @PostMapping("/{id}/process")
    @OrderLogAction
    public OrderStateChangeResponse stateChange(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                @PathVariable String id,
                                                @OrderLogParam @RequestParam("action") Order.OrderAction orderAction) {

        OrderStateChangeBean orderStateChangeBean = orderService.performOrderAction(id, orderAction);

        return toOrderStateChangeResponse(orderStateChangeBean);
    }

    private OrderStateChangeResponse toOrderStateChangeResponse(final OrderStateChangeBean orderStateChangeBean) {

        final OrderStateChange orderStateChange = orderStateChangeBean.getOrderStateChange();
        final OrderStateChange.OrderStateChangeEntry orderStateChangeEntry = orderStateChange.getLastEntry().orElseThrow(() -> {
            throw new ObjectNotFoundException(orderStateChange.getOrderId(), OrderStateChange.class);
        });

        List<PrinterInstructionResponse> printerInstructions = List.of();

        if (orderStateChangeBean.getPrinterInstructions().isPresent()) {
            printerInstructions = toPrinterInstructionResponses(orderStateChangeBean.getPrinterInstructions().get());
        }

        return new OrderStateChangeResponse(orderStateChange.getOrderId(),
                orderStateChangeEntry.getFromState(),
                orderStateChangeEntry.getToState(),
                orderStateChangeEntry.getTimestamp(),
                printerInstructions);
    }

    private List<PrinterInstructionResponse> toPrinterInstructionResponses(PrinterInstructions printerInstructions) {

        return printerInstructions.getPrinterInstructions().stream()
                .map(pi -> new PrinterInstructionResponse(
                        pi.getPrintInstruction(),
                        pi.getPrinterIpAddresses(),
                        pi.getNoOfPrintCopies())).collect(Collectors.toList());
    }

}
