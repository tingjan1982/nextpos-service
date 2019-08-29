package io.nextpos.timecard.data;

import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

public interface UserTimeCardRepository extends PagingAndSortingRepository<UserTimeCard, String> {

    Optional<UserTimeCard> findByClientIdAndUsernameAndTimeCardStatus(String clientId, String username, UserTimeCard.TimeCardStatus timeCardStatus);
}
