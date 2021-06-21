package io.nextpos.reservation.web.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Positive;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class ReservationSettingsRequest {

    @Positive
    private long durationMinutes;

    private List<String> nonReservableTables = new ArrayList<>();
}
