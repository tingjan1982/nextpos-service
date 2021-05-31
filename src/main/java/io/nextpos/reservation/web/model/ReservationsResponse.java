package io.nextpos.reservation.web.model;

import io.nextpos.reservation.data.Reservation;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class ReservationsResponse {

    private Reservation.ReservationType reservationType;

    private List<ReservationResponse> results;

    public ReservationsResponse(Reservation.ReservationType reservationType, List<Reservation> reservations) {

        this.reservationType = reservationType;
        this.results = reservations.stream()
                .map(ReservationResponse::new)
                .collect(Collectors.toList());
    }
}
