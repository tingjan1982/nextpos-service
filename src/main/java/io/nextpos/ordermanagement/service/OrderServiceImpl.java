package io.nextpos.ordermanagement.service;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderRepository;
import io.nextpos.ordermanagement.data.OrderStateChange;
import io.nextpos.ordermanagement.data.OrderStateChangeRepository;
import io.nextpos.shared.exception.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    private final OrderStateChangeRepository orderStateChangeRepository;

    @Autowired
    public OrderServiceImpl(final OrderRepository orderRepository, final OrderStateChangeRepository orderStateChangeRepository) {
        this.orderRepository = orderRepository;
        this.orderStateChangeRepository = orderStateChangeRepository;
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
    public boolean orderExists(final String id) {
        return orderRepository.existsById(id);
    }

    @Override
    public OrderStateChange transitionOrderState(final Order order, final Order.OrderState orderState) {

        final OrderStateChange orderStateChange = orderStateChangeRepository.findById(order.getId())
                .orElse(new OrderStateChange(order.getId()));

        orderStateChange.addStateChange(order.getState(), orderState);
        orderStateChangeRepository.save(orderStateChange);

        order.setState(orderState);

        orderRepository.save(order);

        return orderStateChange;
    }

}
