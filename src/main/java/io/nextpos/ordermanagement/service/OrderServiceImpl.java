package io.nextpos.ordermanagement.service;

import io.nextpos.client.data.Client;
import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.merchandising.data.ProductLevelOffer;
import io.nextpos.merchandising.service.MerchandisingService;
import io.nextpos.ordermanagement.data.*;
import io.nextpos.ordermanagement.event.LineItemStateChangeEvent;
import io.nextpos.ordermanagement.event.OrderStateChangeEvent;
import io.nextpos.ordermanagement.service.bean.UpdateLineItem;
import io.nextpos.shared.exception.BusinessLogicException;
import io.nextpos.shared.exception.GeneralApplicationException;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.service.annotation.MongoTransaction;
import io.nextpos.storage.service.DistributedCounterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@MongoTransaction
public class OrderServiceImpl implements OrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final ShiftService shiftService;

    private final MerchandisingService merchandisingService;

    private final DistributedCounterService distributedCounterService;

    private final OrderRepository orderRepository;

    private final OrderStateChangeRepository orderStateChangeRepository;

    private final ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    public OrderServiceImpl(final ShiftService shiftService, final MerchandisingService merchandisingService, final DistributedCounterService distributedCounterService, final OrderRepository orderRepository, final OrderStateChangeRepository orderStateChangeRepository, final ApplicationEventPublisher applicationEventPublisher) {
        this.shiftService = shiftService;
        this.merchandisingService = merchandisingService;
        this.distributedCounterService = distributedCounterService;
        this.orderRepository = orderRepository;
        this.orderStateChangeRepository = orderStateChangeRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }


    @Override
    public Order createOrder(final Order order) {

        shiftService.getActiveShiftOrThrows(order.getClientId());
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

        LOGGER.info("Date range used to get orders: {}, {}", zonedDateRange.getFromLocalDateTime(), zonedDateRange.getToLocalDateTime());

        return orderRepository.findAllByClientAndDateRangeOrderByCreatedDateDesc(client.getId(),
                zonedDateRange.getFromDate(),
                zonedDateRange.getToDate());
    }

    @Override
    public List<Order> getInflightOrders(final String clientId) {
        final List<Order.OrderState> states = Order.OrderState.inflightStates();

        final Shift activeShift = shiftService.getActiveShiftOrThrows(clientId);

        final Sort sort = Sort.by(Sort.Order.by("state"), Sort.Order.asc("createdDate"));
        return orderRepository.findAllByClientIdAndCreatedDateGreaterThanEqualAndStateIsIn(clientId, activeShift.getStart().getTimestamp(), states, sort);
    }

    @Override
    public List<Order> getOrdersByState(final String clientId, final Order.OrderState orderState) {

        LOGGER.info("Getting in process orders for client: {}", clientId);

        final Sort sort = Sort.by(Sort.Order.desc("createdDate"));
        return orderRepository.findAllByClientIdAndState(clientId, orderState, sort);
    }

    @Override
    public void deleteOrder(final Order order) {
        orderRepository.delete(order);
    }

    @Override
    public Order updateOrderLineItem(final Order order, final UpdateLineItem updateLineItem) {

        order.updateOrderLineItem(updateLineItem, (lineItem) -> {
            final ProductLevelOffer.GlobalProductDiscount globalProductDiscount = updateLineItem.getGlobalProductDiscount();

            if (globalProductDiscount != null) {
                merchandisingService.applyGlobalProductDiscount(lineItem, globalProductDiscount, updateLineItem.getDiscountValue());
            }

            lineItem.updateQuantityAndProductOptions(updateLineItem.getQuantity(),
                    updateLineItem.getOverridePrice(),
                    updateLineItem.getProductOptionSnapshots());
        });

//        final Query query = new Query(where("orderLineItems.id").is(lineItemId));
//        final Update update = new Update().set("orderLineItems.$.quantity", updateOrderLineItemRequest.getQuantity());
//        mongoTemplate.updateFirst(query, update, Order.class);

        return orderRepository.save(order);
    }

    @Override
    public Order deleteOrderLineItem(final Order order, final String lineItemId) {

        order.deleteOrderLineItem(lineItemId);
        return orderRepository.save(order);
    }

    @Override
    public Order addOrderLineItem(final Order order, final OrderLineItem orderLineItem) {

        if (order.isClosed()) {
            throw new BusinessLogicException("message.orderClosed", "Order is closed, cannot add OrderLineItem: " + order.getId());
        }

        order.addOrderLineItem(orderLineItem);

        return orderRepository.save(order);
    }

    @Override
    public OrderStateChangeBean performOrderAction(final String id, final Order.OrderAction orderAction) {

        final CompletableFuture<OrderStateChangeBean> future = new CompletableFuture<>();
        final Order order = this.getOrder(id);
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
    public OrderStateChange transitionOrderState(final Order order, final Order.OrderAction orderAction, Optional<LineItemStateChangeEvent> lineItemStateChangeEvent) {

        final Order.OrderState orderState = orderAction.getValidNextState();

        final OrderStateChange orderStateChange = orderStateChangeRepository.findById(order.getId())
                .orElse(new OrderStateChange(order.getId(), order.getClientId()));

        orderStateChange.addStateChange(order.getState(), orderState);
        orderStateChangeRepository.save(orderStateChange);

        order.setState(orderState);

        lineItemStateChangeEvent.ifPresent(applicationEventPublisher::publishEvent);

        orderRepository.save(order);

        return orderStateChange;
    }

    @Override
    public List<OrderLineItem> prepareLineItems(final String orderId, final List<String> lineItemIds) {
        return publishLineItemEvent(orderId, lineItemIds, Order.OrderAction.PREPARE);
    }

    @Override
    public List<OrderLineItem> deliverLineItems(String orderId, List<String> lineItemIds) {
        return publishLineItemEvent(orderId, lineItemIds, Order.OrderAction.PARTIAL_DELIVER);
    }

    private List<OrderLineItem> publishLineItemEvent(String orderId, List<String> lineItemIds, Order.OrderAction orderAction) {

        if (!CollectionUtils.isEmpty(lineItemIds)) {
            final Order order = this.getOrder(orderId);
            final List<OrderLineItem> orderLineItems = order.getOrderLineItems().stream()
                    .filter(li -> lineItemIds.contains(li.getId()))
                    .collect(Collectors.toList());

            if (!orderLineItems.isEmpty()) {
                LOGGER.info("Sending [{}] LineItemStateChangeEvent: {}", orderAction, orderLineItems);
                applicationEventPublisher.publishEvent(new LineItemStateChangeEvent(this, order, orderAction, orderLineItems));

                this.saveOrder(order);

                return orderLineItems;
            }
        }

        return List.of();
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
        final int counter = distributedCounterService.getNextRotatingCounter(clientId);
        return LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + counter;
    }
}
