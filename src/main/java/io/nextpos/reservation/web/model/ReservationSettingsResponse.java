package io.nextpos.reservation.web.model;

import io.nextpos.reservation.data.ReservationSettings;
import lombok.Data;

import java.util.List;

@Data
public class ReservationSettingsResponse {

    private final String reservationLink;

    private final long durationMinutes;

    private final List<String> nonReservableTables;

    public ReservationSettingsResponse(ReservationSettings reservationSettings, String reservationUrl) {

        reservationLink = reservationUrl + "/reservations/" + reservationSettings.getReservationKey();
        durationMinutes = reservationSettings.getReservationDuration().toMinutes();
        nonReservableTables = reservationSettings.getNonReservableTables();
    }
}
