package io.nextpos.roster.web.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
public class RosterPlanRequest {

    private int year;

    private int month;

    private List<RosterEntryRequest> rosterEntries;

    @Data
    @NoArgsConstructor
    public static class RosterEntryRequest {

        private DayOfWeek dayOfWeek;

        private LocalTime startTime;

        private LocalTime endTime;
    }
}
