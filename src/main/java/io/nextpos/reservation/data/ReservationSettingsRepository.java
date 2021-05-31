package io.nextpos.reservation.data;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ReservationSettingsRepository extends MongoRepository<ReservationSettings, String> {
}
