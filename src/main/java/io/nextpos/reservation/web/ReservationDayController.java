package io.nextpos.reservation.web;

import io.nextpos.client.data.Client;
import io.nextpos.reservation.service.ReservationService;
import io.nextpos.reservation.web.model.ReservationDayResponse;
import io.nextpos.shared.web.ClientResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/reservationDays")
public class ReservationDayController {

    private final ReservationService reservationService;

    @Autowired
    public ReservationDayController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping("/{date}")
    public ReservationDayResponse getReservationDay(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                    @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {

        return new ReservationDayResponse(reservationService.getReservationDay(client, date));
    }
}
