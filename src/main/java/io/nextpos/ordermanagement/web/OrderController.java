package io.nextpos.ordermanagement.web;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientObjectOwnershipService;
import io.nextpos.merchandising.data.Offer;
import io.nextpos.merchandising.data.OrderLevelOffer;
import io.nextpos.merchandising.data.ProductLevelOffer;
import io.nextpos.merchandising.service.MerchandisingService;
import io.nextpos.ordermanagement.data.*;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.ordermanagement.service.ShiftService;
import io.nextpos.ordermanagement.service.bean.UpdateLineItem;
import io.nextpos.ordermanagement.web.factory.OrderCreationFactory;
import io.nextpos.ordermanagement.web.model.*;
import io.nextpos.ordertransaction.service.OrderTransactionService;
import io.nextpos.ordertransaction.web.model.OrderTransactionResponse;
import io.nextpos.reporting.data.DateParameterType;
import io.nextpos.reporting.data.ReportDateParameter;
import io.nextpos.shared.exception.BusinessLogicException;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.web.ClientResolver;
import io.nextpos.shared.web.model.SimpleObjectResponse;
import io.nextpos.shared.web.model.SimpleObjectsResponse;
import io.nextpos.tablelayout.service.TableLayoutService;
import io.nextpos.tablelayout.web.model.TableDetailsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    private final OrderTransactionService orderTransactionService;

    private final ClientObjectOwnershipService clientObjectOwnershipService;

    private final TableLayoutService tableLayoutService;

    private final OrderCreationFactory orderCreationFactory;

    private final MerchandisingService merchandisingService;

    private final ShiftService shiftService;

    @Autowired
    public OrderController(final OrderService orderService, final OrderTransactionService orderTransactionService, final ClientObjectOwnershipService clientObjectOwnershipService, final TableLayoutService tableLayoutService, final OrderCreationFactory orderCreationFactory, final MerchandisingService merchandisingService, final ShiftService shiftService) {
        this.orderService = orderService;
        this.orderTransactionService = orderTransactionService;
        this.clientObjectOwnershipService = clientObjectOwnershipService;
        this.tableLayoutService = tableLayoutService;
        this.orderCreationFactory = orderCreationFactory;
        this.merchandisingService = merchandisingService;
        this.shiftService = shiftService;
    }

    @PostMapping
    public OrderResponse createOrder(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                     @RequestBody OrderRequest orderRequest) {

        Order order = orderCreationFactory.newOrder(client, orderRequest);
        final Order createdOrder = orderService.createOrder(order);

        return toOrderResponse(createdOrder);
    }

    @GetMapping
    public OrdersByRangeResponse getOrdersByDateRange(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                      @RequestParam(name = "dateRange", required = false, defaultValue = "SHIFT") DateParameterType dateParameterType,
                                                      @RequestParam(name = "shiftId", required = false) String shiftId,
                                                      @RequestParam(name = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date fromDate,
                                                      @RequestParam(name = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date toDate) {

        final ReportDateParameter reportDateParameter = resolveDateRange(client, dateParameterType, shiftId, fromDate, toDate);
        final List<Order> orders = orderService.getOrders(client, reportDateParameter.getFromDate(), reportDateParameter.getToDate());

        return toOrdersByRangeResponse(orders, reportDateParameter);
    }

    private OrdersByRangeResponse toOrdersByRangeResponse(final List<Order> orders, final ReportDateParameter reportDateParameter) {

        final List<OrdersByRangeResponse.LightOrderResponse> orderResponses = orders.stream().
                map(o -> new OrdersByRangeResponse.LightOrderResponse(o.getId(),
                        o.getSerialId(),
                        o.getOrderType(),
                        o.getCreatedDate(),
                        o.getState(),
                        o.getTotal(),
                        o.getOrderTotal())).collect(Collectors.toList());

        return new OrdersByRangeResponse(reportDateParameter, orderResponses);
    }

    private ReportDateParameter resolveDateRange(Client client, DateParameterType dateParameterType, final String shiftId, final Date fromDateParam, final Date toDateParam) {

        if (dateParameterType != DateParameterType.SHIFT) {
            return DateParameterType.toReportingParameter(dateParameterType, fromDateParam, toDateParam);
        }

        if (shiftId != null) {
            Shift shift = shiftService.getShift(shiftId);
            return new ReportDateParameter(shift.getStart().toLocalDateTime(), shift.getEnd().toLocalDateTime());
        }

        final Optional<Shift> mostRecentShift = shiftService.getMostRecentShift(client.getId());

        if (mostRecentShift.isPresent()) {
            final Shift shift = mostRecentShift.get();

            return new ReportDateParameter(shift.getStart().toLocalDateTime(), shift.getEnd().toLocalDateTime());
        }

        throw new BusinessLogicException("No date range specified.");
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
        final Map<String, List<OrdersResponse.LightOrderResponse>> orderResponses = orders.stream()
                .map(o -> {
                    String tableLayoutId = o.getTableInfo() != null ? o.getTableInfo().getTableLayoutId() : "NO_LAYOUT";
                    String tableLayoutName = o.getTableInfo() != null ? o.getTableInfo().getTableLayoutName() : "N/A";

                    return new OrdersResponse.LightOrderResponse(o.getId(),
                            o.getOrderType(),
                            tableLayoutId,
                            tableLayoutName,
                            o.getTableInfo().getDisplayName(),
                            o.getDemographicData().getCustomerCount(),
                            o.getCreatedDate(),
                            o.getState(),
                            o.getTotal(),
                            o.getOrderTotal());
                })
                .collect(Collectors.groupingBy(OrdersResponse.LightOrderResponse::getTableLayoutId, Collectors.toList()));

        return new OrdersResponse(orderResponses);
    }

    @GetMapping("/availableTables")
    public TablesResponse getTables(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        final List<String> occupiedTableIds = orderService.getInflightOrders(client.getId()).stream()
                .filter(o -> o.getTableInfo() != null)
                .map(o -> o.getTableInfo().getTableId())
                .collect(Collectors.toList());

        final Map<String, List<TableDetailsResponse>> availableTables = tableLayoutService.getTableLayouts(client).stream()
                .flatMap(tl -> tl.getTables().stream())
                .filter(t -> !occupiedTableIds.contains(t.getId()))
                .collect(Collectors.groupingBy(t -> t.getTableLayout().getId(),
                        Collectors.mapping(TableDetailsResponse::fromTableDetails, Collectors.toList())));

        return new TablesResponse(availableTables);
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable String id) {

        final Order order = orderService.getOrder(id);
        final OrderResponse orderResponse = toOrderResponse(order);

        orderService.getOrderStateChangeByOrderId(id).flatMap(OrderStateChange::getOrderPreparationDuration).ifPresent(orderResponse::setOrderPreparationTime);

        final List<OrderTransactionResponse> transactions = orderTransactionService.getOrderTransactionByOrderId(order.getId()).stream()
                .map(OrderTransactionResponse::toOrderTransactionResponse)
                .collect(Collectors.toList());

        orderResponse.setTransactions(transactions);

        return orderResponse;
    }

    @PostMapping("/{id}")
    public OrderResponse updateOrder(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                     @PathVariable final String id,
                                     @RequestBody OrderRequest orderRequest) {

        final Order order = clientObjectOwnershipService.checkWithClientIdOwnership(client, () -> orderService.getOrder(id));

        orderCreationFactory.updateTableInfoAndDemographicData(order, orderRequest);

        return toOrderResponse(orderService.saveOrder(order));
    }

    @PostMapping("/{id}/applyDiscount")
    public OrderResponse applyOrderDiscount(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                            @PathVariable final String id,
                                            @Valid @RequestBody DiscountRequest discountRequest) {

        final Order order = clientObjectOwnershipService.checkWithClientIdOwnership(client, () -> orderService.getOrder(id));
        final OrderLevelOffer.GlobalOrderDiscount globalOrderDiscount = OrderLevelOffer.GlobalOrderDiscount.valueOf(discountRequest.getOrderDiscount());

        BigDecimal discount = discountRequest.getDiscount();

        if (globalOrderDiscount.getDiscountType() == Offer.DiscountType.PERCENT_OFF) {
            discount = discountRequest.getDiscount().divide(BigDecimal.valueOf(100), 2, RoundingMode.CEILING);
        }

        final Order updatedOrder = merchandisingService.applyGlobalOrderDiscount(order, globalOrderDiscount, discount);

        return toOrderResponse(updatedOrder);
    }

    @PostMapping("/{id}/waiveServiceCharge")
    public OrderResponse waiveServiceCharge(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                            @PathVariable final String id,
                                            @RequestParam(value = "apply", defaultValue = "true") boolean apply) {

        final Order order = clientObjectOwnershipService.checkWithClientIdOwnership(client, () -> orderService.getOrder(id));

        final Order updatedOrder = merchandisingService.updateServiceCharge(order, apply);

        return toOrderResponse(updatedOrder);
    }

    @PostMapping("/{id}/resetOrderOffers")
    public OrderResponse resetOrderOffers(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                          @PathVariable final String id) {

        final Order order = clientObjectOwnershipService.checkWithClientIdOwnership(client, () -> orderService.getOrder(id));

        final Order updatedOrder = merchandisingService.resetOrderOffers(order);

        return toOrderResponse(updatedOrder);
    }

    @PostMapping("/{id}/copy")
    public OrderResponse copyOrder(@PathVariable final String id) {

        Order copiedOrder = orderService.copyOrder(id);
        return toOrderResponse(copiedOrder);
    }

    @DeleteMapping("/{id}")
    public OrderStateChangeResponse deleteOrder(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                @PathVariable final String id) {

        OrderStateChangeBean orderStateChangeBean = orderService.performOrderAction(id, Order.OrderAction.DELETE);

        return toOrderStateChangeResponse(orderStateChangeBean);
    }


    @PostMapping("/{id}/lineitems")
    public OrderResponse AddOrderLineItem(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                          @PathVariable String id,
                                          @Valid @RequestBody OrderLineItemRequest request) {

        final Order order = orderService.getOrder(id);
        final OrderLineItem orderLineItem = orderCreationFactory.newOrderLineItem(client, request);

        if (request.getProductDiscount() != null) {
            BigDecimal discountValue = resolveProductDiscountValue(request.getProductDiscount(), request.getDiscountValue());
            merchandisingService.applyGlobalProductDiscount(orderLineItem, request.getProductDiscount(), discountValue);
        }

        orderService.addOrderLineItem(order, orderLineItem);

        return toOrderResponse(order);
    }

    @PostMapping("/{id}/lineitems/deliver")
    public SimpleObjectsResponse deliverLineItems(@PathVariable final String id,
                                                  @Valid @RequestBody UpdateLineItemsRequest updateLineItemsRequest) {

        final List<OrderLineItem> updatedOrderLineItems = orderService.deliverLineItems(id, updateLineItemsRequest.getLineItemIds());
        final List<SimpleObjectResponse> simpleObjects = updatedOrderLineItems.stream()
                .map(li -> new SimpleObjectResponse(li.getId(), li.getProductSnapshot().getName()))
                .collect(Collectors.toList());

        return new SimpleObjectsResponse(simpleObjects);
    }

    @PatchMapping("/{id}/lineitems/{lineItemId}")
    public OrderResponse updateOrderLineItem(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                             @PathVariable String id,
                                             @PathVariable String lineItemId,
                                             @Valid @RequestBody UpdateOrderLineItemRequest request) {

        final Order order = clientObjectOwnershipService.checkWithClientIdOwnership(client, () -> orderService.getOrder(id));

        final ProductLevelOffer.GlobalProductDiscount productDiscount = request.getProductDiscount();
        BigDecimal discountValue = request.getDiscountValue();

        if (productDiscount != null) {
            discountValue = resolveProductDiscountValue(productDiscount, discountValue);
        }

        final UpdateLineItem updateLineItem = new UpdateLineItem(lineItemId, request.getQuantity(), request.toProductOptionSnapshots(), productDiscount, discountValue);

        final Order updatedOrder = orderService.updateOrderLineItem(order, updateLineItem);

        return toOrderResponse(updatedOrder);
    }

    private BigDecimal resolveProductDiscountValue(ProductLevelOffer.GlobalProductDiscount productDiscount, BigDecimal discountValue) {

        if (productDiscount.getDiscountType() == Offer.DiscountType.PERCENT_OFF) {
            return discountValue.divide(BigDecimal.valueOf(100), 2, RoundingMode.CEILING);
        }

        return discountValue;

    }

    @PostMapping("/{id}/process")
    public OrderStateChangeResponse stateChange(@PathVariable String id, @RequestParam("action") Order.OrderAction orderAction) {

        OrderStateChangeBean orderStateChangeBean = orderService.performOrderAction(id, orderAction);

        return toOrderStateChangeResponse(orderStateChangeBean);
    }

    private OrderStateChangeResponse toOrderStateChangeResponse(final OrderStateChangeBean orderStateChangeBean) {

        final OrderStateChange orderStateChange = orderStateChangeBean.getOrderStateChange();
        final OrderStateChange.OrderStateChangeEntry orderStateChangeEntry = orderStateChange.getLastEntry().orElseThrow(() -> {
            throw new ObjectNotFoundException(orderStateChange.getOrderId(), OrderStateChange.class);
        });

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
                            .map(po -> String.format("%s: %s ($%s)", po.getOptionName(), po.getOptionValue(), po.getOptionPrice()))
                            .collect(Collectors.joining(", "));

                    return new OrderResponse.OrderLineItemResponse(li.getId(),
                            li.getProductSnapshot().getId(),
                            li.getState(),
                            li.getProductSnapshot().getName(),
                            options,
                            li.getProductPriceWithOptions().getAmount(),
                            li.getQuantity(),
                            li.getLineItemSubTotal(),
                            li.getSubTotal(),
                            li.getDiscountedSubTotal(),
                            li.getAppliedOfferInfo(),
                            li.getModifiedDate());

                }).collect(Collectors.toList());

        return new OrderResponse(order.getId(),
                order.getSerialId(),
                order.getOrderType(),
                order.getTableInfo(),
                order.getTableInfo().getDisplayName(),
                order.getServedBy(),
                order.getCreatedDate(),
                order.getModifiedDate(),
                order.getState(),
                order.getTotal(),
                order.getDiscountedTotal(),
                order.getDiscount(),
                order.getServiceCharge(),
                order.getOrderTotal(),
                order.getCurrency(),
                orderLineItems,
                order.getMetadata(),
                order.getDemographicData(),
                order.getAppliedOfferInfo(),
                order.getOrderDuration());
    }

}
