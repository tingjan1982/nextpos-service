package io.nextpos.ordermanagement.service;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Optional;

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
        return orderRepository.save(order);
    }

    @Override
    public Optional<Order> getOrder(final String id) {
        return orderRepository.findById(id);
    }

    @Override
    public boolean orderExists(final String id) {
        return orderRepository.existsById(id);
    }

}
