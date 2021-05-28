package io.nextpos.reservation.web.model;

import io.nextpos.reservation.data.Reservation;
import io.nextpos.reservation.service.ReservationService;
import io.nextpos.shared.exception.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@RestController
@RequestMapping("/web-reservations")
public class WebReservationController {

    private final ReservationService reservationService;

    private final ConcurrentMap<String, ReservationResponse> reservations = new ConcurrentHashMap<>();

    @Autowired
    public WebReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping("/{id}")
    public ReservationResponse getReservation(@PathVariable String id) {

        return reservations.computeIfAbsent(id, _id -> new ReservationResponse(_id,
                new ReservationResponse.ClientInfo("Ron Xinyi", "0227092313", "台北市基隆路二段26號"),
                new Date(),
                "Joe",
                "0988120232",
                4,
                2,
                "birthday event",
                List.of(),
                Reservation.ReservationStatus.BOOKED
        ));
    }

    @PostMapping("/{id}/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmReservation(@PathVariable String id) {

        final ReservationResponse reservationResponse = reservations.get(id);

        if (reservationResponse == null) {
            throw new ObjectNotFoundException(id, Reservation.class);
        }

        reservationResponse.setStatus(Reservation.ReservationStatus.CONFIRMED);
    }

    @PostMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelReservation(@PathVariable String id) {

        final ReservationResponse reservationResponse = reservations.get(id);

        if (reservationResponse == null) {
            throw new ObjectNotFoundException(id, Reservation.class);
        }

        reservationResponse.setStatus(Reservation.ReservationStatus.CANCELLED);
    }
}
