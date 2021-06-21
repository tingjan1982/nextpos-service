package io.nextpos.reservation.service;

import io.nextpos.reservation.data.ReservationSettings;

public interface ReservationSettingsService {

    ReservationSettings getReservationSettings(String id);

    ReservationSettings saveReservationSettings(ReservationSettings reservationSettings);
}
