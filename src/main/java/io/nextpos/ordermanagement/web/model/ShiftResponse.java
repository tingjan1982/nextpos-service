package io.nextpos.ordermanagement.web.model;

import io.nextpos.ordermanagement.data.Shift;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShiftResponse {

    private String id;

    private Shift.ShiftStatus shiftStatus;

    private Date timestamp;

    private String who;

    private BigDecimal balance;
}
