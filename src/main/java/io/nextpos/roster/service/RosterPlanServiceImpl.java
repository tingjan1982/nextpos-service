package io.nextpos.roster.service;

import io.nextpos.calendarevent.data.CalendarEvent;
import io.nextpos.calendarevent.service.CalendarEventService;
import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.roster.data.RosterPlan;
import io.nextpos.roster.data.RosterPlanRepository;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import io.nextpos.shared.util.DateTimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@ChainedTransaction
public class RosterPlanServiceImpl implements RosterPlanService {

    private final RosterPlanRepository rosterPlanRepository;

    private final CalendarEventService calendarEventService;

    @Autowired
    public RosterPlanServiceImpl(RosterPlanRepository rosterPlanRepository, CalendarEventService calendarEventService) {
        this.rosterPlanRepository = rosterPlanRepository;
        this.calendarEventService = calendarEventService;
    }

    @Override
    public RosterPlan saveRosterPlan(RosterPlan rosterPlan) {
        return rosterPlanRepository.save(rosterPlan);
    }

    @Override
    public RosterPlan getRosterPlan(String id) {
        return rosterPlanRepository.findById(id).orElseThrow(() -> {
            throw new ObjectNotFoundException(id, RosterPlan.class);
        });
    }

    @Override
    public List<CalendarEvent> createCalendarEventsFromRosterPlan(Client client, RosterPlan rosterPlan) {

        final YearMonth rosterMonth = rosterPlan.getRosterMonth();
        return rosterPlan.getRosterEntries().entrySet().stream()
                .map(entry -> {
                    final DayOfWeek dayOfWeek = entry.getKey();
                    final LocalDate firstInMonth = LocalDate.now().with(rosterMonth).with(TemporalAdjusters.firstInMonth(dayOfWeek));
                    final LocalDate lastInMonth = LocalDate.now().with(rosterMonth).with(TemporalAdjusters.lastInMonth(dayOfWeek));
                    LocalDate nextInMonth = firstInMonth;
                    List<CalendarEvent> createdEvents = new ArrayList<>();

                    while (nextInMonth.compareTo(lastInMonth) <= 0) {
                        final List<CalendarEvent> calendarEvents = createCalendarEvents(client, nextInMonth, entry.getValue());
                        nextInMonth = nextInMonth.with(TemporalAdjusters.next(dayOfWeek));
                        createdEvents.addAll(calendarEvents);
                    }

                    return createdEvents;
                })
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private List<CalendarEvent> createCalendarEvents(Client client, LocalDate dayOfMonth, List<RosterPlan.RosterEntry> rosterEntries) {

        return rosterEntries.stream().map(re -> {
            final LocalDateTime startTime = LocalDateTime.of(dayOfMonth, re.getStartTime());
            final LocalDateTime endTime = LocalDateTime.of(dayOfMonth, re.getEndTime());
            final CalendarEvent shift = new CalendarEvent(client.getId(), CalendarEvent.EventType.ROSTER, "Work", DateTimeUtil.toDate(client.getZoneId(), startTime), DateTimeUtil.toDate(client.getZoneId(), endTime));
            return calendarEventService.saveCalendarEvent(shift);
        }).collect(Collectors.toList());
    }

    @Override
    public CalendarEvent assignStaffMember(CalendarEvent calendarEvent, ClientUser clientUser) {

        final CalendarEvent.EventResource eventResource = toEventResource(clientUser);
        return calendarEventService.updateEventResource(calendarEvent, eventResource);
    }

    @Override
    public CalendarEvent removeStaffMember(CalendarEvent calendarEvent) {
        return calendarEventService.removeEventResource(calendarEvent);
    }

    @Override
    public List<CalendarEvent> getCalendarEventsForStaffMember(Client client, ClientUser clientUser, YearMonth yearMonth) {

        CalendarEvent.EventResource eventResource = toEventResource(clientUser);
        return calendarEventService.getCalendarEventsForEventResource(client, yearMonth, CalendarEvent.EventType.ROSTER, eventResource);
    }

    private CalendarEvent.EventResource toEventResource(ClientUser clientUser) {
        return new CalendarEvent.EventResource(clientUser.getId().getUsername(), CalendarEvent.ResourceType.STAFF, clientUser.getName());
    }
}
