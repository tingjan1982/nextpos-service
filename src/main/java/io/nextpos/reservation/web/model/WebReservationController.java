package io.nextpos.reservation.web.model;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.reservation.data.Reservation;
import io.nextpos.reservation.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/web-reservations")
public class WebReservationController {

    private final ReservationService reservationService;

    private final ClientService clientService;

    @Autowired
    public WebReservationController(ReservationService reservationService, ClientService clientService) {
        this.reservationService = reservationService;
        this.clientService = clientService;
    }

    @GetMapping("/{id}")
    public ReservationResponse getReservation(@PathVariable String id) {

        final Reservation reservation = reservationService.getReservation(id);
        final Client client = clientService.getClientOrThrows(reservation.getClientId());

        return new ReservationResponse(client, reservation);
    }

    @PostMapping("/{id}/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmReservation(@PathVariable String id) {

        final Reservation reservation = reservationService.getReservation(id);
        reservationService.confirmReservation(reservation);
    }

    @PostMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelReservation(@PathVariable String id) {

        final Reservation reservation = reservationService.getReservation(id);
        reservationService.cancelReservation(reservation);
    }
}
