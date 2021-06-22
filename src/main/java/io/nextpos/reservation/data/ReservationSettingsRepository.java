package io.nextpos.reservation.data;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ReservationSettingsRepository extends MongoRepository<ReservationSettings, String> {

    Optional<ReservationSettings> findByReservationKey(String reservationKey);
}
