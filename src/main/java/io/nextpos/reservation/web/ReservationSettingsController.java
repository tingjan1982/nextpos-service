package io.nextpos.reservation.web;

import io.nextpos.client.data.Client;
import io.nextpos.reservation.data.ReservationSettings;
import io.nextpos.reservation.service.ReservationSettingsService;
import io.nextpos.reservation.web.model.ReservationSettingsRequest;
import io.nextpos.reservation.web.model.ReservationSettingsResponse;
import io.nextpos.shared.web.ClientResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/reservationSettings")
public class ReservationSettingsController {

    private final ReservationSettingsService reservationSettingsService;

    @Autowired
    public ReservationSettingsController(ReservationSettingsService reservationSettingsService) {
        this.reservationSettingsService = reservationSettingsService;
    }

    @GetMapping("/me")
    public ReservationSettingsResponse getReservationSettings(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        return toResponse(reservationSettingsService.getReservationSettings(client.getId()));
    }

    @PostMapping("/me")
    public ReservationSettingsResponse saveReservationSettings(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                               @Valid @RequestBody ReservationSettingsRequest request) {

        final ReservationSettings reservationSettings = reservationSettingsService.getReservationSettings(client.getId());

        updateFromRequest(reservationSettings, request);

        return toResponse(reservationSettingsService.saveReservationSettings(reservationSettings));
    }

    private void updateFromRequest(ReservationSettings reservationSettings, ReservationSettingsRequest request) {

        final Duration duration = Duration.of(request.getDurationMinutes(), ChronoUnit.MINUTES);
        reservationSettings.setReservationDuration(duration);
        reservationSettings.setNonReservableTables(request.getNonReservableTables());
    }

    private ReservationSettingsResponse toResponse(ReservationSettings reservationSettings) {

        return new ReservationSettingsResponse(reservationSettings);
    }
}
