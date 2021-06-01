package io.nextpos.reservation.web.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class ReservationRequest {

    @NotNull
    private LocalDateTime reservationDate;

    @NotBlank
    private String name;

    @NotBlank
    private String phoneNumber;

    @Positive
    private int people;

    private int kid;

    private List<String> tableIds = new ArrayList<>();

    private String note;
}
