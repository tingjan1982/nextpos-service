package io.nextpos.ordermanagement.service;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    @Autowired
    public OrderServiceImpl(final OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }


    @Override
    public Order createOrder(final Order order) {

        order.setState(Order.OrderState.NEW);
        
        return orderRepository.save(order);
    }

    @Override
    public Order getOrder(final String id) {
        return orderRepository.getOne(id);
    }

    @Override
    public boolean orderExists(final String id) {
        return orderRepository.existsById(id);
    }

    @Override
    public void deleteOrder(final Order order) {
        orderRepository.delete(order);
    }
}
