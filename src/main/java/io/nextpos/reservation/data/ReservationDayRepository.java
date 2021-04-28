package io.nextpos.reservation.data;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ReservationDayRepository extends MongoRepository<ReservationDay, String> {

    Optional<ReservationDay> findByClientIdAndDate(String clientId, LocalDate date);
}
