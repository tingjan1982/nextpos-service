package io.nextpos.ordermanagement.service;

import io.nextpos.client.data.Client;
import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.merchandising.data.ProductLevelOffer;
import io.nextpos.merchandising.service.MerchandisingService;
import io.nextpos.ordermanagement.data.*;
import io.nextpos.ordermanagement.event.LineItemStateChangeEvent;
import io.nextpos.ordermanagement.event.OrderStateChangeEvent;
import io.nextpos.ordermanagement.service.bean.LineItemOrdering;
import io.nextpos.ordermanagement.service.bean.UpdateLineItem;
import io.nextpos.shared.aspect.WebSocketClientOrder;
import io.nextpos.shared.aspect.WebSocketClientOrders;
import io.nextpos.shared.auth.AuthenticationHelper;
import io.nextpos.shared.exception.BusinessLogicException;
import io.nextpos.shared.exception.GeneralApplicationException;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.service.annotation.MongoTransaction;
import io.nextpos.workingarea.service.WorkingAreaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
@MongoTransaction
public class OrderServiceImpl implements OrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final ShiftService shiftService;

    private final MerchandisingService merchandisingService;

    private final WorkingAreaService workingAreaService;

    private final OrderRepository orderRepository;

    private final OrderStateChangeRepository orderStateChangeRepository;

    private final MongoTemplate mongoTemplate;

    private final ApplicationEventPublisher applicationEventPublisher;

    private final AuthenticationHelper authenticationHelper;

    @Autowired
    public OrderServiceImpl(final ShiftService shiftService, final MerchandisingService merchandisingService, WorkingAreaService workingAreaService, final OrderRepository orderRepository, final OrderStateChangeRepository orderStateChangeRepository, MongoTemplate mongoTemplate, final ApplicationEventPublisher applicationEventPublisher, AuthenticationHelper authenticationHelper) {
        this.shiftService = shiftService;
        this.merchandisingService = merchandisingService;
        this.workingAreaService = workingAreaService;
        this.orderRepository = orderRepository;
        this.orderStateChangeRepository = orderStateChangeRepository;
        this.mongoTemplate = mongoTemplate;
        this.applicationEventPublisher = applicationEventPublisher;
        this.authenticationHelper = authenticationHelper;
    }


    @Override
    @WebSocketClientOrders
    public Order createOrder(final Order order) {

        shiftService.getActiveShiftOrThrows(order.getClientId());

        final String serialId = this.generateSerialId(order.getClientId());
        order.setSerialId(serialId);

        return orderRepository.save(order);
    }

    @Override
    public Order saveOrder(final Order order) {
        return orderRepository.save(order);
    }

    @Override
    public Order getOrder(final String id) {
        return orderRepository.findById(id).orElseThrow(() -> {
            throw new ObjectNotFoundException(id, Order.class);
        });
    }

    @Override
    public List<Order> getOrders(final Client client, ZonedDateRange zonedDateRange) {
        return getOrders(client, zonedDateRange, OrderCriteria.instance());
    }

    @Override
    public List<Order> getOrders(final Client client, ZonedDateRange zonedDateRange, OrderCriteria orderCriteria) {

        LOGGER.info("Date range used to get orders: {}, {}", zonedDateRange.getFromLocalDateTime(), zonedDateRange.getToLocalDateTime());

        final Criteria criteria = where("clientId").is(client.getId())
                .and("createdDate").gte(zonedDateRange.getFromDate()).lt(zonedDateRange.getToDate());
        orderCriteria.decorateCriteria(criteria);

        final Query query = Query.query(criteria)
                .with(Sort.by(Sort.Order.asc("modifiedDate")));

        return mongoTemplate.find(query, Order.class);
    }

    @Override
    public List<Order> getInflightOrders(final String clientId) {
        final List<Order.OrderState> states = Order.OrderState.inflightStates();

        final Shift activeShift = shiftService.getActiveShiftOrThrows(clientId);
        final Sort sort = Sort.by(Sort.Order.by("state"), Sort.Order.asc("createdDate"));

        return orderRepository.findAllByClientIdAndCreatedDateGreaterThanEqualAndStateIsIn(clientId, activeShift.getStart().getTimestamp(), states, sort);
    }

    @Override
    public List<Order> getOrdersByStates(final String clientId, final List<Order.OrderState> orderStates) {

        LOGGER.info("Getting in process orders for client: {}", clientId);

        final Shift activeShift = shiftService.getActiveShiftOrThrows(clientId);
        final Sort sort = Sort.by(Sort.Order.desc("createdDate"));

        return orderRepository.findAllByClientIdAndCreatedDateGreaterThanEqualAndStateIsIn(clientId, activeShift.getStart().getTimestamp(), orderStates, sort);
    }

    @Override
    public List<Order> getInStoreInFlightOrders(String clientId) {

        final Optional<Shift> activeShift = shiftService.getActiveShift(clientId);

        if (activeShift.isEmpty()) {
            return List.of();
        }

        return orderRepository.findAllByClientIdAndCreatedDateGreaterThanEqualAndOrderTypeAndStateIsIn(clientId,
                activeShift.get().getStart().getTimestamp(),
                Order.OrderType.IN_STORE,
                Order.OrderState.inflightStates());
    }

    @Override
    public InProcessOrderLineItems getInProcessOrderLineItems(String clientId) {

        final Map<String, List<InProcessOrderLineItem>> groupedOrders = this.getOrdersByStates(clientId,
                        List.of(Order.OrderState.IN_PROCESS, Order.OrderState.SETTLED, Order.OrderState.COMPLETED)).stream()
                .flatMap(o -> {
                    final Map<String, List<OrderLineItem>> lineItemsGroupedByWorkingArea =
                            OrderVisitors.get(o, OrderVisitors.OrderLineItemGrouper.instance(workingAreaService));

                    return lineItemsGroupedByWorkingArea.entrySet().stream()
                            .map(e -> InProcessOrderLineItem.orderLineItems(o, e.getKey(), e.getValue()))
                            .flatMap(Collection::stream);
                })
                .sorted(InProcessOrderLineItem.getComparator())
                .collect(Collectors.groupingBy(InProcessOrderLineItem::getWorkingArea));

        return new InProcessOrderLineItems(groupedOrders);
    }

    @Override
    public InProcessOrders getInProcessOrders(String clientId) {

        final List<InProcessOrder> orders = this.getOrdersByStates(clientId,
                        List.of(Order.OrderState.IN_PROCESS, Order.OrderState.SETTLED, Order.OrderState.COMPLETED)).stream()
                .filter(o -> o.getOrderLineItems().stream().anyMatch(li -> li.getState().isPreparing()))
                .map(InProcessOrder::new)
                .sorted(InProcessOrder.getComparator())
                .collect(Collectors.toList());

        return new InProcessOrders(orders);
    }

    @Override
    public void markAllLineItemsAsPrepared(String clientId) {

        final List<Order> orders = this.getOrdersByStates(clientId,
                List.of(Order.OrderState.IN_PROCESS, Order.OrderState.SETTLED, Order.OrderState.COMPLETED));

        orders.stream()
                .flatMap(o -> o.getOrderLineItems().stream())
                .forEach(li -> {
                    if (li.getState().isPreparing()) {
                        li.setState(OrderLineItem.LineItemState.PREPARED);
                    }
                });

        orderRepository.saveAll(orders);
    }

    @Override
    @WebSocketClientOrders
    public Order moveOrder(String sourceOrderId, String targetOrderId) {

        final Order sourceOrder = this.getOrder(sourceOrderId);
        final Order targetOrder = this.getOrder(targetOrderId);

        if (sourceOrder.isClosed() || targetOrder.isClosed()) {
            throw new BusinessLogicException("message.cannotMove", "Either source or target order is closed.");
        }

        sourceOrder.getOrderLineItems().forEach(targetOrder::addOrderLineItem);

        targetOrder.mergeDemographicData(sourceOrder.getDemographicData());

        this.markOrderAsDeleted(sourceOrderId, false);
        return this.saveOrder(targetOrder);
    }

    @Override
    @WebSocketClientOrders
    public void deleteOrder(final String orderId) {
        orderRepository.deleteById(orderId);
    }

    @Override
    @WebSocketClientOrders
    public void markOrderAsDeleted(String orderId, boolean shiftAudit) {

        this.performOrderAction(orderId, Order.OrderAction.DELETE);
        final Order order = this.getOrder(orderId);

        if (shiftAudit) {
            final Shift activeShift = shiftService.getActiveShiftOrThrows(order.getClientId());
            final String username = authenticationHelper.resolveCurrentUsername();
            order.getOrderLineItems().forEach(li -> activeShift.addDeletedLineItem(order, li, username));
            shiftService.saveShift(activeShift);
        }

        order.deleteAllOrderLineItems();
        this.saveOrder(order);
    }

    @Override
    @WebSocketClientOrder
    public Order updateOrderLineItem(final Order order, final UpdateLineItem updateLineItem) {

        order.productSetOrder().updateOrderLineItem(updateLineItem.getLineItemId(), (lineItem) -> {
            final ProductLevelOffer.GlobalProductDiscount globalProductDiscount = updateLineItem.getGlobalProductDiscount();

            if (globalProductDiscount != null) {
                merchandisingService.applyGlobalProductDiscount(lineItem, globalProductDiscount, updateLineItem.getDiscountValue());
            }

            lineItem.getProductSnapshot().setSku(updateLineItem.getSku());
            lineItem.updateQuantityAndProductOptions(updateLineItem.getQuantity(),
                    updateLineItem.getOverridePrice(),
                    updateLineItem.getProductOptionSnapshots());
        });

        return orderRepository.save(order);
    }

    @Override
    @WebSocketClientOrder
    public Order updateOrderLineItemPrice(Order order, String lineItemId, BigDecimal overridePrice) {

        final OrderLineItem lineItem = order.productSetOrder().updateOrderLineItem(lineItemId, (li) -> {
            li.removeOffer();
            li.getProductSnapshot().setOverridePrice(overridePrice);
        });

        final String username = authenticationHelper.resolveCurrentUsername();
        final Shift activeShift = shiftService.getActiveShiftOrThrows(order.getClientId());

        if (overridePrice != null && overridePrice.compareTo(BigDecimal.ZERO) == 0) {
            activeShift.addDeletedLineItem(order, lineItem, username);
        } else {
            activeShift.removeDeletedLineItem(lineItem);
        }

        shiftService.saveShift(activeShift);

        return orderRepository.save(order);
    }

    @Override
    @WebSocketClientOrder
    public Order deleteOrderLineItem(final Order order, final String lineItemId) {

        final OrderLineItem orderLineItem = order.getOrderLineItem(lineItemId);
        order.productSetOrder().deleteOrderLineItem(orderLineItem);

        final String username = authenticationHelper.resolveCurrentUsername();
        final Shift activeShift = shiftService.getActiveShiftOrThrows(order.getClientId());
        activeShift.addDeletedLineItem(order, orderLineItem, username);
        shiftService.saveShift(activeShift);

        return orderRepository.save(order);
    }

    @Override
    @WebSocketClientOrder
    public Order addOrderLineItem(Client client, final Order order, final OrderLineItem orderLineItem) {

        if (order.isClosed()) {
            throw new BusinessLogicException("message.orderClosed", "Order is closed, cannot add OrderLineItem: " + order.getId());
        }

        order.productSetOrder().addOrderLineItem(orderLineItem);

        return merchandisingService.computeOffers(client, order);
    }

    @Override
    public OrderStateChangeBean performOrderAction(final String id, final Order.OrderAction orderAction) {

        final Order order = this.getOrder(id);
        return this.performOrderAction(order, orderAction);
    }

    @Override
    public OrderStateChangeBean performOrderAction(final Order order, final Order.OrderAction orderAction) {

        final CompletableFuture<OrderStateChangeBean> future = new CompletableFuture<>();
        applicationEventPublisher.publishEvent(new OrderStateChangeEvent(this, order, orderAction, future));

        return this.getOrderStateChangeBeanFromFuture(future);
    }

    @Override
    public Optional<OrderStateChange> getOrderStateChangeByOrderId(final String orderId) {
        return orderStateChangeRepository.findById(orderId);
    }

    private OrderStateChangeBean getOrderStateChangeBeanFromFuture(CompletableFuture<OrderStateChangeBean> future) {

        try {
            return future.get(15, TimeUnit.SECONDS);

        } catch (GeneralApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralApplicationException(e.getMessage());
        }
    }

    @Override
    @WebSocketClientOrder
    @WebSocketClientOrders
    public OrderStateChange transitionOrderState(final Order order, final Order.OrderAction orderAction, Optional<LineItemStateChangeEvent> lineItemStateChangeEvent) {

        Order.OrderState nextOrderState = orderAction.getValidNextState();
        final OrderStateChange orderStateChange = orderStateChangeRepository.findById(order.getId())
                .orElse(new OrderStateChange(order.getId(), order.getClientId()));

        switch (nextOrderState) {
            case PREV_FROM_STATE:
                nextOrderState = orderStateChange.getPreviousEntry().getFromState();
                orderStateChange.addStateChange(order.getState(), nextOrderState);
                break;
            case RETAIN_STATE:
                nextOrderState = order.getState();
                break;
            default:
                orderStateChange.addStateChange(order.getState(), nextOrderState);
                break;
        }

        orderStateChangeRepository.save(orderStateChange);

        order.setState(nextOrderState);

        lineItemStateChangeEvent.ifPresent(applicationEventPublisher::publishEvent);

        orderRepository.save(order);

        return orderStateChange;
    }

    @Override
    @WebSocketClientOrder
    public Order prepareLineItems(final String orderId, final List<String> lineItemIds) {
        return publishLineItemEvent(orderId, lineItemIds, Order.OrderAction.PREPARE);
    }

    @Override
    @WebSocketClientOrder
    public Order deliverLineItems(String orderId, List<String> lineItemIds) {
        return publishLineItemEvent(orderId, lineItemIds, Order.OrderAction.PARTIAL_DELIVER);
    }

    private Order publishLineItemEvent(String orderId, List<String> lineItemIds, Order.OrderAction orderAction) {

        final Order order = this.getOrder(orderId);

        if (!CollectionUtils.isEmpty(lineItemIds)) {

            final List<OrderLineItem> orderLineItems = order.getOrderLineItems().stream()
                    .filter(li -> lineItemIds.contains(li.getId()))
                    .collect(Collectors.toList());

            if (!orderLineItems.isEmpty()) {
                LOGGER.info("Sending [{}] LineItemStateChangeEvent: {}", orderAction, orderLineItems);
                applicationEventPublisher.publishEvent(new LineItemStateChangeEvent(this, order, orderAction, orderLineItems));

                this.saveOrder(order);
            }
        }

        return order;
    }

    @Override
    @WebSocketClientOrder
    public Order moveLineItems(Order fromOrder, Order toOrder, List<String> lineItemIds) {

        lineItemIds.stream()
                .map(fromOrder::getOrderLineItem)
                .forEach(li -> {
                    final OrderLineItem copy = li.copy();
                    toOrder.productSetOrder().addOrderLineItem(copy);
                    fromOrder.productSetOrder().deleteOrderLineItem(li);
                });

        this.saveOrder(toOrder);

        return this.saveOrder(fromOrder);
    }

    @Override
    public Order copyOrder(final String id) {

        final Order order = this.getOrder(id);
        Order copiedOrder = order.copy();
        copiedOrder.setSerialId(this.generateSerialId(order.getClientId()));
        copiedOrder.setState(Order.OrderState.OPEN);
        copiedOrder.getOrderLineItems().forEach(li -> li.setState(OrderLineItem.LineItemState.OPEN));

        return this.saveOrder(copiedOrder);
    }

    @Override
    public String generateSerialId(String clientId) {

        final String orderIdPrefix = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        OrderIdCounter orderIdCounter = mongoTemplate.findAndModify(Query.query(where("clientId").is(clientId).and("orderIdPrefix").is(orderIdPrefix)),
                new Update().inc("counter", 1L),
                FindAndModifyOptions.options().returnNew(true),
                OrderIdCounter.class);

        if (orderIdCounter == null) {
            orderIdCounter = new OrderIdCounter(clientId, orderIdPrefix, 1);
            mongoTemplate.save(orderIdCounter);
        }

        return orderIdCounter.getOrderId();
    }

    @Override
    public void orderLineItems(List<LineItemOrdering> lineItemOrderings) {

        Map<String, Order> orders = new HashMap<>();
        AtomicInteger index = new AtomicInteger(1);
        for (LineItemOrdering lineItemOrdering : lineItemOrderings) {
            Order order = orders.computeIfAbsent(lineItemOrdering.getOrderId(), this::getOrder);
            order.findOrderLineItem(lineItemOrdering.getLineItemId()).ifPresent(li -> li.setOrder(index.getAndIncrement()));
        }

        orders.values().forEach(this::saveOrder);
    }

    @Override
    public void reorder(List<String> orderIds) {

        AtomicInteger index = new AtomicInteger(1);
        orderIds.forEach(oid -> {
            final Order order = this.getOrder(oid);
            order.setOrder(index.getAndIncrement());
            this.saveOrder(order);
        });
    }
}
