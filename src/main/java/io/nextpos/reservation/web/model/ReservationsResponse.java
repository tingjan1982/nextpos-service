package io.nextpos.reservation.web.model;

import io.nextpos.reservation.data.Reservation;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class ReservationsResponse {

    private Reservation.ReservationStatus reservationStatus;

    private List<ReservationResponse> results;

    public ReservationsResponse(Reservation.ReservationStatus reservationStatus, List<Reservation> reservations) {

        this.reservationStatus = reservationStatus;
        this.results = reservations.stream()
                .map(ReservationResponse::new)
                .collect(Collectors.toList());
    }
}
