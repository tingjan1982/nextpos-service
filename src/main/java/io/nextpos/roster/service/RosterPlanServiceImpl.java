package io.nextpos.roster.service;

import io.nextpos.calendarevent.data.CalendarEvent;
import io.nextpos.calendarevent.data.CalendarEventSeries;
import io.nextpos.calendarevent.service.CalendarEventService;
import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.roster.data.RosterPlan;
import io.nextpos.roster.data.RosterPlanRepository;
import io.nextpos.shared.exception.GeneralApplicationException;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import io.nextpos.shared.util.DateTimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@ChainedTransaction
public class RosterPlanServiceImpl implements RosterPlanService {

    private final CalendarEventService calendarEventService;

    private final RosterPlanRepository rosterPlanRepository;

    @Autowired
    public RosterPlanServiceImpl(CalendarEventService calendarEventService, RosterPlanRepository rosterPlanRepository) {
        this.calendarEventService = calendarEventService;
        this.rosterPlanRepository = rosterPlanRepository;
    }

    @Deprecated
    @Override
    public RosterPlan saveRosterPlan(RosterPlan rosterPlan) {
        return rosterPlanRepository.save(rosterPlan);
    }

    @Deprecated
    @Override
    public RosterPlan getRosterPlan(String id) {
        return rosterPlanRepository.findById(id).orElseThrow(() -> {
            throw new ObjectNotFoundException(id, RosterPlan.class);
        });
    }

    @Deprecated
    @Override
    public List<RosterPlan> getRosterPlans(Client client) {
        return rosterPlanRepository.findAllByClientId(client.getId());
    }

    @Deprecated
    @Override
    public void deleteRosterPlan(RosterPlan rosterPlan) {

        this.deleteRosterPlanEvents(rosterPlan);
        rosterPlanRepository.delete(rosterPlan);
    }

    @Override
    public List<CalendarEvent> createRosterEvent(Client client, CalendarEventSeries.EventRepeat eventRepeat, CalendarEvent baseCalendarEvent) {

        switch (eventRepeat) {
            case WEEKLY:
                final CalendarEventSeries calendarEventSeries = new CalendarEventSeries(client.getId(), eventRepeat);
                calendarEventService.saveCalendarEventSeries(calendarEventSeries);

                List<CalendarEvent> calendarEvents = new ArrayList<>();
                baseCalendarEvent.setEventSeries(calendarEventSeries);
                calendarEvents.add(this.createRosterEvent(baseCalendarEvent));

                final ZoneId zoneId = client.getZoneId();
                LocalDateTime nextStartTime = DateTimeUtil.toLocalDateTime(zoneId, baseCalendarEvent.getStartTime());
                LocalDateTime nextEndTime = DateTimeUtil.toLocalDateTime(zoneId, baseCalendarEvent.getEndTime());
                LocalDateTime lastDayOfTheSeries = nextStartTime.with(TemporalAdjusters.lastInMonth(nextStartTime.getDayOfWeek()));

                while (nextStartTime.compareTo(lastDayOfTheSeries) < 0) {
                    nextStartTime = nextStartTime.with(TemporalAdjusters.next(nextStartTime.getDayOfWeek()));
                    nextEndTime = nextEndTime.with(TemporalAdjusters.next(nextStartTime.getDayOfWeek()));

                    final CalendarEvent copiedCalendarEvent = baseCalendarEvent.copy(
                            DateTimeUtil.toDate(zoneId, nextStartTime),
                            DateTimeUtil.toDate(zoneId, nextEndTime));
                    copiedCalendarEvent.setEventSeries(calendarEventSeries);
                    calendarEvents.add(this.createRosterEvent(copiedCalendarEvent));
                }

                return calendarEvents;
            case NONE:
                return List.of(this.createRosterEvent(baseCalendarEvent));
            default:
                throw new GeneralApplicationException("Not all EventRepeat type is accommodated: " + eventRepeat.name());
        }
    }

    private CalendarEvent createRosterEvent(CalendarEvent calendarEvent) {
        return calendarEventService.saveCalendarEvent(calendarEvent);
    }

    @Override
    public List<CalendarEvent> getRosterEvents(Client client, YearMonth yearMonth) {

        final Date startOfMonth = DateTimeUtil.toDate(client.getZoneId(), yearMonth.atDay(1).atStartOfDay());
        final Date endOfMonth = DateTimeUtil.toDate(client.getZoneId(), yearMonth.atEndOfMonth().atTime(23, 59, 59));

        return calendarEventService.getCalendarEvents(client.getId(), CalendarEvent.EventType.ROSTER, startOfMonth, endOfMonth);
    }

    @Override
    public List<CalendarEvent> getTodaysClientUserRosterEvents(Client client, ClientUser clientUser) {

        final CalendarEvent.EventResource eventResource = toEventResource(clientUser);
        final Date from = Date.from(LocalDate.now().atStartOfDay().atZone(client.getZoneId()).toInstant());
        final Date to = Date.from(LocalDate.now().atTime(23, 59, 59).atZone(client.getZoneId()).toInstant());

        return calendarEventService.getCalendarEvents(client.getId(), CalendarEvent.EventType.ROSTER, eventResource, from, to);
    }

    @Override
    public CalendarEvent getRosterEvent(String id) {
        return calendarEventService.getCalendarEvent(id);
    }

    @Override
    public CalendarEvent updateRosterEvent(CalendarEvent calendarEvent, LocalTime startTime, LocalTime endTime, long daysDiff, boolean applyToSeries) {
        return calendarEventService.updateCalendarEvent(calendarEvent, startTime, endTime, daysDiff, applyToSeries);
    }

    @Override
    public CalendarEvent updateRosterEventResources(CalendarEvent calendarEvent, List<CalendarEvent.EventResource> eventResources) {
        return calendarEventService.updateEventResources(calendarEvent, eventResources);
    }

    @Override
    public void deleteRosterEvent(String id, boolean applyToSeries) {
        final CalendarEvent calendarEvent = calendarEventService.getCalendarEvent(id);
        calendarEventService.deleteCalendarEvent(calendarEvent, applyToSeries);
    }

    @Deprecated
    @Override
    public List<CalendarEvent> createRosterPlanEvents(Client client, RosterPlan rosterPlan) {

        final YearMonth rosterMonth = rosterPlan.getRosterMonth();
        final List<CalendarEvent> rosterEvents = rosterPlan.getRosterEntries().entrySet().stream()
                .map(entry -> {
                    final DayOfWeek dayOfWeek = entry.getKey();
                    final LocalDate firstInMonth = LocalDate.now().with(rosterMonth).with(TemporalAdjusters.firstInMonth(dayOfWeek));
                    final LocalDate lastInMonth = LocalDate.now().with(rosterMonth).with(TemporalAdjusters.lastInMonth(dayOfWeek));
                    LocalDate nextInMonth = firstInMonth;
                    List<CalendarEvent> createdEvents = new ArrayList<>();

                    while (nextInMonth.compareTo(lastInMonth) <= 0) {
                        final List<CalendarEvent> calendarEvents = createRosterPlanEvents(client, nextInMonth, rosterPlan, entry.getValue());
                        nextInMonth = nextInMonth.with(TemporalAdjusters.next(dayOfWeek));
                        createdEvents.addAll(calendarEvents);
                    }

                    return createdEvents;
                })
                .flatMap(List::stream)
                .collect(Collectors.toList());

        rosterPlan.setStatus(RosterPlan.RosterPlanStatus.LOCKED);
        this.saveRosterPlan(rosterPlan);

        return rosterEvents;
    }

    private List<CalendarEvent> createRosterPlanEvents(Client client, LocalDate dayOfMonth, RosterPlan rosterPlan, List<RosterPlan.RosterEntry> rosterEntries) {

        return rosterEntries.stream().map(entry -> {
            LocalDateTime startTime = LocalDateTime.of(dayOfMonth, entry.getStartTime());
            LocalDateTime endTime = LocalDateTime.of(dayOfMonth, entry.getEndTime());

            if (entry.getEndTime().isBefore(entry.getStartTime())) {
                endTime = LocalDateTime.of(dayOfMonth.plusDays(1), entry.getEndTime());
            }

            final CalendarEvent shift = new CalendarEvent(
                    client.getId(),
                    client.getZoneId(),
                    CalendarEvent.EventType.ROSTER,
                    "Work",
                    CalendarEvent.EventOwner.createWithOwnerId(rosterPlan.getId(), CalendarEvent.OwnerType.ROSTER),
                    DateTimeUtil.toDate(client.getZoneId(), startTime),
                    DateTimeUtil.toDate(client.getZoneId(), endTime));
            return calendarEventService.saveCalendarEvent(shift);

        }).collect(Collectors.toList());
    }

    @Deprecated
    @Override
    public List<CalendarEvent> getRosterPlanEvents(RosterPlan rosterPlan) {

        return calendarEventService.getCalendarEventsForEventOwner(rosterPlan.getClientId(), rosterPlan.getId(), CalendarEvent.OwnerType.ROSTER);
    }

    @Deprecated
    @Override
    public void deleteRosterPlanEvents(RosterPlan rosterPlan) {

        rosterPlan.setStatus(RosterPlan.RosterPlanStatus.ACTIVE);
        this.saveRosterPlan(rosterPlan);

        calendarEventService.deleteCalendarEvents(rosterPlan.getClientId(), rosterPlan.getId(), CalendarEvent.OwnerType.ROSTER);
    }

    @Deprecated
    @Override
    public CalendarEvent assignRosterPlanEventToStaffMember(CalendarEvent calendarEvent, ClientUser clientUser) {

        final CalendarEvent.EventResource eventResource = toEventResource(clientUser);
        return calendarEventService.addEventResource(calendarEvent, eventResource);
    }

    @Deprecated
    @Override
    public CalendarEvent removeStaffMemberFromRosterPlanEvent(CalendarEvent calendarEvent, ClientUser clientUser) {
        return calendarEventService.removeEventResource(calendarEvent, toEventResource(clientUser));
    }

    @Deprecated
    @Override
    public CalendarEvent updateRosterPlanEventStaffMembers(CalendarEvent calendarEvent, List<ClientUser> clientUsers) {
        final List<CalendarEvent.EventResource> eventResources = clientUsers.stream()
                .map(this::toEventResource)
                .collect(Collectors.toList());

        return calendarEventService.updateEventResources(calendarEvent, eventResources);
    }

    private CalendarEvent.EventResource toEventResource(ClientUser clientUser) {
        return new CalendarEvent.EventResource(clientUser.getId().getUsername(), CalendarEvent.ResourceType.STAFF);
    }
}
