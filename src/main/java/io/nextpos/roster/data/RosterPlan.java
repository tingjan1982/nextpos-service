package io.nextpos.roster.data;

import io.nextpos.shared.model.MongoBaseObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

@Document
@CompoundIndexes({@CompoundIndex(name = "unique_per_client_index", def = "{'clientId': 1, 'rosterMonth': 1}", unique = true)})
@Data
@EqualsAndHashCode(callSuper = true)
public class RosterPlan extends MongoBaseObject {

    @Id
    private String id;

    private String clientId;

    private YearMonth rosterMonth;

    private RosterPlanStatus status = RosterPlanStatus.ACTIVE;

    private AtomicInteger internalCounter = new AtomicInteger(1);

    private Map<DayOfWeek, List<RosterEntry>> rosterEntries = new TreeMap<>();


    public RosterPlan(String clientId, YearMonth rosterMonth) {
        this.id = ObjectId.get().toString();
        this.clientId = clientId;
        this.rosterMonth = rosterMonth;
    }

    public void addRosterEntry(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {

        final List<RosterEntry> rosterEntriesOfDay = this.rosterEntries.computeIfAbsent(dayOfWeek, dow -> new ArrayList<>());
        final String rosterEntryId = this.id + "-" + internalCounter.getAndIncrement();
        final RosterEntry rosterEntry = new RosterEntry(rosterEntryId, dayOfWeek, startTime, endTime);

        rosterEntriesOfDay.add(rosterEntry);
    }

    public void removeRosterEntry(String rosterEntryId) {
        this.rosterEntries.values().forEach(entries -> entries.removeIf(entry -> entry.getId().equals(rosterEntryId)));
    }

    @Data
    @AllArgsConstructor
    public static class RosterEntry {

        private String id;

        private DayOfWeek dayOfWeek;

        private LocalTime startTime;

        private LocalTime endTime;
    }

    public enum RosterPlanStatus {

        /**
         * Default state, still allow modification.
         */
        ACTIVE,

        /**
         * Locked so that it cannot be changed.
         */
        LOCKED
    }
}
