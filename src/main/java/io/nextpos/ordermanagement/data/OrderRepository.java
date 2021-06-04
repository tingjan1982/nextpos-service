package io.nextpos.ordermanagement.data;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Date;
import java.util.List;

public interface OrderRepository extends MongoRepository<Order, String> {

    List<Order> findAllByClientIdAndCreatedDateGreaterThanEqualAndStateIsIn(String clientId, Date date, List<Order.OrderState> orderStates, Sort sort);

    List<Order> findAllByClientIdAndCreatedDateGreaterThanEqualAndOrderTypeAndStateIsIn(String clientId, Date date, Order.OrderType orderType, List<Order.OrderState> orderStates);

    long countByClientIdAndStateIn(String clientId, List<Order.OrderState> orderStates);

    /**
     * Kept as implementation reference. Not used.
     */
    @Query(value = "{$and: [{ 'clientId': ?0 }, { 'createdDate': { $gte: ?1, $lt: ?2 } }, { 'tables.tableName': ?3 }]}")
    @Deprecated
    List<Order> findAllByClientAndDateRangeAndTableName(String clientId, Date fromDate, Date toDate, String tableName);
}
