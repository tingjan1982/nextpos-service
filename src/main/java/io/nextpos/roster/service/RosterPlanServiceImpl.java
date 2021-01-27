package io.nextpos.roster.service;

import io.nextpos.calendarevent.data.CalendarEvent;
import io.nextpos.calendarevent.data.CalendarEventSeries;
import io.nextpos.calendarevent.service.CalendarEventService;
import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.shared.exception.GeneralApplicationException;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import io.nextpos.shared.util.DateTimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@ChainedTransaction
public class RosterPlanServiceImpl implements RosterPlanService {

    private final CalendarEventService calendarEventService;

    @Autowired
    public RosterPlanServiceImpl(CalendarEventService calendarEventService) {
        this.calendarEventService = calendarEventService;
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

    private CalendarEvent.EventResource toEventResource(ClientUser clientUser) {
        return new CalendarEvent.EventResource(clientUser.getId().getUsername(), CalendarEvent.ResourceType.STAFF);
    }
}
