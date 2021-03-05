package io.nextpos.timecard.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
public class TimeCardReport {

    private List<UserShift> userTimeCards;


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserShift {

        private String id;

        private String username;

        private String displayName;

        private int totalShifts;

        private BigDecimal totalHours;
    }
}

