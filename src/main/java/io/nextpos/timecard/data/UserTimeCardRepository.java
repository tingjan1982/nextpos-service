package io.nextpos.timecard.data;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserTimeCardRepository extends PagingAndSortingRepository<UserTimeCard, String> {

    Optional<UserTimeCard> findByClientIdAndUsernameAndTimeCardStatus(String clientId, String username, UserTimeCard.TimeCardStatus timeCardStatus);

    Optional<UserTimeCard> findFirstByClientIdAndUsernameOrderByCreatedDateDesc(String clientId, String username);

    @Deprecated
    @Query(value = "{$and: [{ 'clientId': ?0 }, { 'username': ?1 }, { 'clockIn': { $gte: ?2, $lt: ?3 } }]}")
    List<UserTimeCard> findAllByClientIdAndUsernameAndClockInDateRange(String clientId, String username, LocalDate from, LocalDate to, Sort sort);

    @Query(value = "{$and: [{ 'clientId': ?0 }, { 'userId': ?1 }, { 'clockIn': { $gte: ?2, $lt: ?3 } }]}")
    List<UserTimeCard> findAllByClientIdAndUserIdAndClockInDateRange(String clientId, String username, LocalDate from, LocalDate to, Sort sort);
}
