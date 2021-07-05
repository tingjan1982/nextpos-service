package io.nextpos.reservation.web.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ReservationDelayRequest {

    private int minutes;
}
