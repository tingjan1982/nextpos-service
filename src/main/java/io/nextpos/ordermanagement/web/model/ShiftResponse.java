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

    private String clientId;

    private Shift.ShiftStatus shiftStatus;

    private ShiftDetailsResponse open;

    private ShiftDetailsResponse close;

    private BigDecimal difference;

    @Data
    @AllArgsConstructor
    public static class ShiftDetailsResponse {

        private Date timestamp;

        private String who;

        private BigDecimal balance;
    }
}
