package io.nextpos.ordermanagement.service;

import io.nextpos.ordermanagement.data.*;
import io.nextpos.ordermanagement.web.model.UpdateOrderLineItemRequest;
import io.nextpos.shared.exception.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    private final OrderStateChangeRepository orderStateChangeRepository;

    private final MongoTemplate mongoTemplate;

    @Autowired
    public OrderServiceImpl(final OrderRepository orderRepository, final OrderStateChangeRepository orderStateChangeRepository, final MongoTemplate mongoTemplate) {
        this.orderRepository = orderRepository;
        this.orderStateChangeRepository = orderStateChangeRepository;
        this.mongoTemplate = mongoTemplate;
    }


    @Override
    public Order createOrder(final Order order) {
        return orderRepository.save(order);
    }

    @Override
    public Order getOrder(final String id) {
        return orderRepository.findById(id).orElseThrow(() -> {
            throw new ObjectNotFoundException(id, Order.class);
        });
    }

    @Override
    public void deleteOrder(final Order order) {
        orderRepository.delete(order);
    }

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
    public OrderStateChange transitionOrderState(final Order order, final Order.OrderState orderState) {

        final OrderStateChange orderStateChange = orderStateChangeRepository.findById(order.getId())
                .orElse(new OrderStateChange(order.getId(), order.getClientId()));

        orderStateChange.addStateChange(order.getState(), orderState);
        orderStateChangeRepository.save(orderStateChange);

        order.setState(orderState);

        orderRepository.save(order);

        return orderStateChange;
    }

}
