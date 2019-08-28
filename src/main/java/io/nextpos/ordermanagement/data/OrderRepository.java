package io.nextpos.ordermanagement.data;

import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Date;
import java.util.List;

public interface OrderRepository extends PagingAndSortingRepository<Order, String> {

    List<Order> findAllByClientIdAndTableIdIsNotNullAndCreatedDateGreaterThanEqualAndStateIsIn(String clientId, Date date, Order.OrderState... orderStates);
}
