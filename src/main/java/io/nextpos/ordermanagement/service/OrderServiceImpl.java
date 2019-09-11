package io.nextpos.ordermanagement.service;

import io.nextpos.ordermanagement.data.*;
import io.nextpos.ordermanagement.event.LineItemStateChangeEvent;
import io.nextpos.ordermanagement.web.model.UpdateOrderLineItemRequest;
import io.nextpos.shared.exception.ObjectNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final ShiftService shiftService;

    private final OrderRepository orderRepository;

    private final OrderStateChangeRepository orderStateChangeRepository;

    private final ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    public OrderServiceImpl(final ShiftService shiftService, final OrderRepository orderRepository, final OrderStateChangeRepository orderStateChangeRepository, final ApplicationEventPublisher applicationEventPublisher) {
        this.shiftService = shiftService;
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
    public List<Order> getInflightOrders(final String clientId) {
        final Order.OrderState[] states = new Order.OrderState[] {
                Order.OrderState.OPEN,
                Order.OrderState.IN_PROCESS,
                Order.OrderState.DELIVERED,
                Order.OrderState.SETTLED,
                Order.OrderState.REFUNDED
        };
        final Shift activeShift = shiftService.getActiveShiftOrThrows(clientId);

        return orderRepository.findAllByClientIdAndTableIdIsNotNullAndCreatedDateGreaterThanEqualAndStateIsIn(clientId, activeShift.getStart().getTimestamp(), states);
    }

    @Override
    public void deleteOrder(final Order order) {
        orderRepository.delete(order);
    }

    // todo: extract request object out of service layer.
    @Override
    public Order updateOrderLineItem(final String id, final String lineItemId, final UpdateOrderLineItemRequest updateOrderLineItemRequest) {

        final Order order = this.getOrder(id);
        order.updateOrderLineItem(lineItemId, updateOrderLineItemRequest.getQuantity());

//        final Query query = new Query(where("orderLineItems.id").is(lineItemId));
//        final Update update = new Update().set("orderLineItems.$.quantity", updateOrderLineItemRequest.getQuantity());
//        mongoTemplate.updateFirst(query, update, Order.class);

        return orderRepository.save(order);
    }

    @Override
    public Order addOrderLineItem(final Order order, final OrderLineItem orderLineItem) {

        order.addOrderLineItem(orderLineItem);

        return orderRepository.save(order);
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
    public List<OrderLineItem> deliverLineItems(String orderId, List<String> lineItemIds) {

        if (!CollectionUtils.isEmpty(lineItemIds)) {
            final Order order = this.getOrder(orderId);
            final List<OrderLineItem> orderLineItems = order.getOrderLineItems().stream()
                    .filter(li -> lineItemIds.contains(li.getId()))
                    .collect(Collectors.toList());

            if (!orderLineItems.isEmpty()) {
                LOGGER.info("Marking line items as delivered: {}", orderLineItems);
                applicationEventPublisher.publishEvent(new LineItemStateChangeEvent(this, order, Order.OrderAction.PARTIAL_DELIVER, orderLineItems));

                this.saveOrder(order);

                return orderLineItems;
            }
        }

        return List.of();
    }
}
