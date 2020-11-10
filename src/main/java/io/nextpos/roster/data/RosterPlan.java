package io.nextpos.roster.data;

import io.nextpos.shared.model.MongoBaseObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
public class RosterPlan extends MongoBaseObject {

    @Id
    private String id;

    private String clientId;

    private RosterPlanStatus status;

    private YearMonth rosterMonth;

    private Map<DayOfWeek, List<RosterEntry>> rosterEntries = new HashMap<>();


    public RosterPlan(String clientId, YearMonth rosterMonth) {
        this.clientId = clientId;
        this.rosterMonth = rosterMonth;

        this.status = RosterPlanStatus.PENDING;
    }

    public void addRosterEntry(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {

        final List<RosterEntry> rosterEntriesOfDay = this.rosterEntries.computeIfAbsent(dayOfWeek, dow -> new ArrayList<>());
        final RosterEntry rosterEntry = new RosterEntry(dayOfWeek, startTime, endTime);
        rosterEntriesOfDay.add(rosterEntry);
    }

    @Data
    @AllArgsConstructor
    public static class RosterEntry {

        private DayOfWeek dayOfWeek;

        private LocalTime startTime;

        private LocalTime endTime;
    }

    public enum RosterPlanStatus {

        /**
         * Not fulfilled yet.
         */
        PENDING,

        /**
         * Fulfilled by a staff.
         */
        PLANNED
    }
}
