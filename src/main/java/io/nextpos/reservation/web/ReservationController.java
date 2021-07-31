package io.nextpos.reservation.web;

import io.nextpos.client.data.Client;
import io.nextpos.membership.service.MembershipService;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.web.OrderController;
import io.nextpos.ordermanagement.web.model.OrderRequest;
import io.nextpos.ordermanagement.web.model.OrderResponse;
import io.nextpos.reservation.data.Reservation;
import io.nextpos.reservation.service.ReservationService;
import io.nextpos.reservation.web.model.*;
import io.nextpos.shared.exception.BusinessLogicException;
import io.nextpos.shared.util.DateTimeUtil;
import io.nextpos.shared.web.ClientResolver;
import io.nextpos.tablelayout.data.TableLayout;
import io.nextpos.tablelayout.service.TableLayoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    private final TableLayoutService tableLayoutService;

    private final MembershipService membershipService;

    private final OrderController orderController;

    @Autowired
    public ReservationController(ReservationService reservationService, TableLayoutService tableLayoutService, MembershipService membershipService, OrderController orderController) {
        this.reservationService = reservationService;
        this.tableLayoutService = tableLayoutService;
        this.membershipService = membershipService;
        this.orderController = orderController;
    }

    @PostMapping
    public ReservationResponse createReservation(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                 @Valid @RequestBody ReservationRequest request) {

        Reservation reservation = fromRequest(client, request);

        return toResponse(client, reservationService.saveReservation(client, reservation));
    }

    private Reservation fromRequest(Client client, ReservationRequest request) {

        final List<TableLayout.TableDetails> tables = request.getTableIds().stream()
                .map(tableLayoutService::getTableDetailsOrThrows)
                .collect(Collectors.toList());

        final Date reservationDate = DateTimeUtil.toDate(client.getZoneId(), request.getReservationDate());
        final Reservation reservation = Reservation.newReservation(client.getId(), reservationDate, request.getSourceOfOrigin(), tables);

        reservation.updateBookingDetails(request.getName(), request.getPhoneNumber(), request.getPeople(), request.getKid());
        reservation.setNote(request.getNote());

        membershipService.updateMembership(request.getMembershipId(), reservation::setMembership);

        return reservation;
    }

    @GetMapping
    public ReservationsResponse getReservations(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                @RequestParam("reservationDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reservationDate,
                                                @RequestParam(value = "reservationStatus", required = false) Reservation.ReservationStatus reservationStatus) {

        final List<Reservation> reservations = reservationService.getReservationsByDateAndStatus(client, reservationDate, reservationStatus);

        return new ReservationsResponse(reservationStatus, reservations);
    }

    @GetMapping("/byMonth")
    public ReservationsResponse getReservationsByYearMonth(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                           @RequestParam("yearMonth") YearMonth yearMonth) {

        final List<Reservation> reservations = reservationService.getReservationsByDateRange(client, yearMonth);

        return new ReservationsResponse(reservations);
    }

    @GetMapping("/byDateRange")
    public ReservationsResponse getReservationsByYearMonth(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                           @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                                                           @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
                                                           @RequestParam("status") List<Reservation.ReservationStatus> statuses) {

        final Date reservationStartDate = DateTimeUtil.toDate(client.getZoneId(), startDate);
        final Date reservationEndDate = DateTimeUtil.toDate(client.getZoneId(), endDate);
        final List<Reservation> reservations = reservationService.getReservationsByDateRange(client,
                reservationStartDate,
                reservationEndDate,
                statuses);

        return new ReservationsResponse(reservations);
    }

    @GetMapping("/availableTables")
    public AvailableTablesResponse getAvailableTables(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                      @RequestParam("reservationDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime reservationDate,
                                                      @RequestParam(value = "reservationId", required = false) String reservationId) {

        final List<String> results = reservationService.getAvailableReservableTables(client, reservationDate, reservationId).stream()
                .map(TableLayout.TableDetails::getId)
                .collect(Collectors.toList());

        return new AvailableTablesResponse(results);
    }

    @GetMapping("/{id}")
    public ReservationResponse getReservation(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                              @PathVariable String id) {

        return toResponse(client, reservationService.getReservation(id));
    }

    @PostMapping("/{id}")
    public ReservationResponse updateReservation(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                 @PathVariable String id,
                                                 @Valid @RequestBody ReservationRequest request) {

        final Reservation reservation = reservationService.getReservation(id);

        updateFromRequest(client, reservation, request);

        return toResponse(client, reservationService.saveReservation(client, reservation));
    }

    private void updateFromRequest(Client client, Reservation reservation, ReservationRequest request) {

        final Date reservationDate = DateTimeUtil.toDate(client.getZoneId(), request.getReservationDate());
        reservation.setStartDate(reservationDate);
        reservation.updateBookingDetails(request.getName(), request.getPhoneNumber(), request.getPeople(), request.getKid());

        final List<TableLayout.TableDetails> tables = request.getTableIds().stream()
                .map(tableLayoutService::getTableDetailsOrThrows)
                .collect(Collectors.toList());

        reservation.updateTableAllocationAndStatus(tables);
        reservation.setNote(request.getNote());

        membershipService.updateMembership(request.getMembershipId(), reservation::setMembership);
    }

    @PostMapping("/{id}/sendNotification")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sendNotification(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client, @PathVariable String id) {

        Reservation reservation = reservationService.getReservation(id);

        reservationService.sendReservationNotification(client, reservation);
    }

    @PostMapping("/{id}/seat")
    public OrderResponse seatReservation(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client, @PathVariable String id) {

        Reservation reservation = reservationService.getReservation(id);

        if (!EnumSet.of(Reservation.ReservationStatus.BOOKED, Reservation.ReservationStatus.CONFIRMED).contains(reservation.getStatus())) {
            throw new BusinessLogicException("Reservation is not in the correct state: " + reservation.getStatus());
        }

        final Order.DemographicData demographicData = new Order.DemographicData();
        demographicData.setMale(reservation.getPeople());
        demographicData.setKid(reservation.getKid());

        final String membershipId = reservation.getMembership() != null ? reservation.getMembership().getId() : null;

        final OrderResponse response = orderController.createOrder(client, new OrderRequest(
                Order.OrderType.IN_STORE,
                reservation.getTableAllocations().stream().map(Reservation.TableAllocation::getTableId).collect(Collectors.toList()),
                demographicData,
                membershipId,
                List.of()
        ));

        reservationService.seatReservation(reservation);

        return response;
    }

    @PostMapping("/{id}/delay")
    public ReservationResponse seat(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client, @PathVariable String id,
                                    @RequestBody ReservationDelayRequest request) {

        Reservation reservation = reservationService.getReservation(id);
        final Reservation updatedReservation = reservationService.delayReservation(client, reservation, request.getMinutes());

        return toResponse(client, updatedReservation);
    }

    private ReservationResponse toResponse(Client client, Reservation reservation) {

        return new ReservationResponse(client, reservation);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReservation(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                  @PathVariable String id) {

        final Reservation reservation = reservationService.getReservation(id);
        reservationService.cancelReservation(reservation);
    }
}
