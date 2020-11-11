package io.nextpos.roster.web.model;

import io.nextpos.roster.data.RosterPlan;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class RosterPlanRequest {

    private int year;

    private int month;

    private List<RosterPlan.RosterEntry> rosterEntries;
}
