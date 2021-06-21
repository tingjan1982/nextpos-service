package io.nextpos.reservation.web.model;

import io.nextpos.reservation.data.ReservationSettings;
import lombok.Data;

import java.util.List;

@Data
public class ReservationSettingsResponse {

    private final long durationMinutes;

    private final List<String> nonReservableTables;

    public ReservationSettingsResponse(ReservationSettings reservationSettings) {

        durationMinutes = reservationSettings.getReservationDuration().toMinutes();
        nonReservableTables = reservationSettings.getNonReservableTables();
    }
}
