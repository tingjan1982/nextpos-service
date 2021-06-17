package io.nextpos.reservation.web.model;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.client.web.model.ClientResponse;
import io.nextpos.reservation.data.Reservation;
import io.nextpos.reservation.service.ReservationService;
import io.nextpos.reservation.web.ReservationController;
import io.nextpos.tablelayout.data.TableLayout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/web-reservations")
public class WebReservationController {

    private final ReservationService reservationService;

    private final ClientService clientService;

    private final ReservationController reservationController;


    @Autowired
    public WebReservationController(ReservationService reservationService, ClientService clientService, ReservationController reservationController) {
        this.reservationService = reservationService;
        this.clientService = clientService;
        this.reservationController = reservationController;
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

    @GetMapping("/clients/{id}")
    public ClientResponse getClient(@PathVariable String id) {

        return new ClientResponse(clientService.getClientOrThrows(id));
    }

    @PostMapping("/clients/{id}/findTables")
    public AvailableTablesResponse findTables(@PathVariable String id,
                                              @Valid @RequestBody FindTablesRequest request) {

        final Client client = clientService.getClientOrThrows(id);

        final List<String> availableTables = reservationService.getAvailableReservableTables(client, request.getReservationDate()).stream()
                .filter(td -> td.getCapacity() >= request.getPeople())
                .map(TableLayout.TableDetails::getId)
                .collect(Collectors.toList());

        return new AvailableTablesResponse(availableTables);
    }

    @PostMapping("/clients/{id}/reservations")
    public ReservationResponse createReservation(@PathVariable String id,
                                                 @Valid @RequestBody ReservationRequest request) {

        final Client client = clientService.getClientOrThrows(id);

        return reservationController.createReservation(client, request);
    }
}
