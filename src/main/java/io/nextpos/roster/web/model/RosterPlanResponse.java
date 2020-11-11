package io.nextpos.roster.web.model;

import io.nextpos.roster.data.RosterPlan;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class RosterPlanResponse {

    private String id;

    private YearMonth rosterMonth;

    private RosterPlan.RosterPlanStatus status;

    private Map<DayOfWeek, List<RosterPlan.RosterEntry>> rosterEntries;

    public RosterPlanResponse(RosterPlan rosterPlan) {
        id = rosterPlan.getId();
        rosterMonth = rosterPlan.getRosterMonth();
        status = rosterPlan.getStatus();
        rosterEntries = rosterPlan.getRosterEntries();
    }
}
