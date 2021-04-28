package io.nextpos.reservation.web;

import io.nextpos.client.data.Client;
import io.nextpos.reservation.data.Reservation;
import io.nextpos.reservation.service.ReservationService;
import io.nextpos.reservation.web.model.ReservationRequest;
import io.nextpos.reservation.web.model.ReservationResponse;
import io.nextpos.shared.util.DateTimeUtil;
import io.nextpos.shared.web.ClientResolver;
import io.nextpos.tablelayout.data.TableLayout;
import io.nextpos.tablelayout.service.TableLayoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    private final TableLayoutService tableLayoutService;

    @Autowired
    public ReservationController(ReservationService reservationService, TableLayoutService tableLayoutService) {
        this.reservationService = reservationService;
        this.tableLayoutService = tableLayoutService;
    }

    @PostMapping
    public ReservationResponse createReservation(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                 @Valid @RequestBody ReservationRequest request) {

        Reservation reservation = fromRequest(client, request);

        return new ReservationResponse(reservationService.saveReservation(client, reservation));
    }

    private Reservation fromRequest(Client client, ReservationRequest request) {

        final List<TableLayout.TableDetails> tables = request.getTableIds().stream()
                .map(tableLayoutService::getTableDetailsOrThrows)
                .collect(Collectors.toList());

        final Date reservationDate = DateTimeUtil.toDate(client.getZoneId(), request.getReservationDate());
        final Reservation reservation = Reservation.normalReservation(client.getId(), reservationDate, tables);
        reservation.updateBookingDetails(request.getName(), request.getPhoneNumber(), request.getPeople(), request.getKid());
        reservation.setNote(request.getNote());

        return reservation;
    }

    @GetMapping("/{id}")
    public ReservationResponse getReservation(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                              @PathVariable String id) {

        return new ReservationResponse(reservationService.getReservation(id));
    }

    @PostMapping("/{id}")
    public ReservationResponse updateReservation(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                 @PathVariable String id,
                                                 @Valid @RequestBody ReservationRequest request) {

        final Reservation reservation = reservationService.getReservation(id);

        updateFromRequest(client, reservation, request);

        return new ReservationResponse(reservationService.saveReservation(client, reservation));
    }

    private void updateFromRequest(Client client, Reservation reservation, ReservationRequest request) {

        final Date reservationDate = DateTimeUtil.toDate(client.getZoneId(), request.getReservationDate());
        reservation.setReservationDate(reservationDate);
        reservation.updateBookingDetails(request.getName(), request.getPhoneNumber(), request.getPeople(), request.getKid());

        final List<TableLayout.TableDetails> tables = request.getTableIds().stream()
                .map(tableLayoutService::getTableDetailsOrThrows)
                .collect(Collectors.toList());

        reservation.updateTableAllocation(tables);
        reservation.setNote(request.getNote());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReservation(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                  @PathVariable String id) {

        reservationService.deleteReservation(id);
    }
}
