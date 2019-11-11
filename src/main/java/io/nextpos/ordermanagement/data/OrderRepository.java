package io.nextpos.ordermanagement.data;

import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Date;
import java.util.List;

public interface OrderRepository extends PagingAndSortingRepository<Order, String> {

    List<Order> findAllByClientIdAndTableInfoIsNotNullAndCreatedDateGreaterThanEqualAndStateIsIn(String clientId, Date date, List<Order.OrderState> orderStates, Sort sort);
}
