package io.nextpos.reservation.web.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class FindTablesRequest {

    @NotNull
    private LocalDateTime reservationDate;

    @Positive
    private int people;
}
