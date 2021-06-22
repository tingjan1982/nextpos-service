package io.nextpos.reservation.service;

import io.nextpos.reservation.data.ReservationSettings;

public interface ReservationSettingsService {

    ReservationSettings getReservationSettings(String id);

    ReservationSettings getReservationSettingsByReservationKey(String reservationKey);

    ReservationSettings saveReservationSettings(ReservationSettings reservationSettings);
}
