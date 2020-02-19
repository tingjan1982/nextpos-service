package io.nextpos.ordermanagement.data;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

public interface OrderRepository extends PagingAndSortingRepository<Order, String> {

    List<Order> findAllByClientIdAndCreatedDateGreaterThanEqualAndStateIsIn(String clientId, Date date, List<Order.OrderState> orderStates, Sort sort);

    long countByClientIdAndStateIn(String clientId, List<Order.OrderState> orderStates);

    @Query(value = "{$and: [{ 'clientId': ?0 }, { 'createdDate': { $gte: ?1, $lt: ?2 } }]}")
    List<Order> findAllByClientAndDateRangeOrderByCreatedDateDesc(String clientId, LocalDateTime fromDate, LocalDateTime toDate);
}
