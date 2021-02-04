package io.nextpos.roster.service;

import io.nextpos.calendarevent.data.CalendarEvent;
import io.nextpos.calendarevent.data.CalendarEventSeries;
import io.nextpos.calendarevent.service.CalendarEventService;
import io.nextpos.client.data.Client;
import io.nextpos.shared.exception.GeneralApplicationException;
import io.nextpos.shared.util.DateTimeUtil;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Component
public class EventSeriesCreatorImpl implements EventSeriesCreator {

    private final CalendarEventService calendarEventService;

    public EventSeriesCreatorImpl(CalendarEventService calendarEventService) {
        this.calendarEventService = calendarEventService;
    }

    @Override
    public List<CalendarEvent> createEventSeriesEvent(Client client, CalendarEvent baseCalendarEvent, CalendarEventSeries.EventRepeat eventRepeat, LocalDateTime repeatEndDate) {

        final ZoneId zoneId = client.getZoneId();
        LocalDateTime resolvedRepeatEndDate = resolveRepeatEndDate(repeatEndDate);

        final CalendarEventSeries calendarEventSeries = new CalendarEventSeries(client.getId(), client.getZoneId(), eventRepeat, resolvedRepeatEndDate);
        calendarEventService.saveCalendarEventSeries(calendarEventSeries);

        List<CalendarEvent> calendarEvents = new ArrayList<>();
        baseCalendarEvent.setEventSeries(calendarEventSeries);
        calendarEvents.add(this.createRosterEvent(baseCalendarEvent));

        LocalDateTime nextStartTime = DateTimeUtil.toLocalDateTime(zoneId, baseCalendarEvent.getStartTime());
        LocalDateTime nextEndTime = DateTimeUtil.toLocalDateTime(zoneId, baseCalendarEvent.getEndTime());
        LocalDateTime lastDayOfTheSeries = lastDayOfTheSeries(nextStartTime, eventRepeat);

        while (nextStartTime.compareTo(lastDayOfTheSeries) < 0 && nextStartTime.compareTo(resolvedRepeatEndDate) < 0) {
            nextStartTime = resolveNextTime(nextStartTime, eventRepeat);
            nextEndTime = resolveNextTime(nextEndTime, eventRepeat);

            final CalendarEvent copiedCalendarEvent = baseCalendarEvent.copy(
                    DateTimeUtil.toDate(zoneId, nextStartTime),
                    DateTimeUtil.toDate(zoneId, nextEndTime));
            copiedCalendarEvent.setEventSeries(calendarEventSeries);
            calendarEvents.add(this.createRosterEvent(copiedCalendarEvent));
        }

        return calendarEvents;
    }

    private LocalDateTime resolveRepeatEndDate(LocalDateTime repeatEndDate) {

        final LocalDateTime endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59);

        if (repeatEndDate == null || repeatEndDate.compareTo(endOfMonth) > 0) {
            return endOfMonth;
        } else {
            return repeatEndDate;
        }
    }

    private LocalDateTime lastDayOfTheSeries(LocalDateTime nextStartTime, CalendarEventSeries.EventRepeat eventRepeat) {

        switch (eventRepeat) {
            case DAILY:
                return nextStartTime.with(TemporalAdjusters.lastDayOfMonth());
            case WEEKLY:
                return nextStartTime.with(TemporalAdjusters.lastInMonth(nextStartTime.getDayOfWeek()));
            default:
                throw new GeneralApplicationException("Not all EventRepeat type is accommodated: " + eventRepeat.name());
        }

    }

    private LocalDateTime resolveNextTime(LocalDateTime nextDateTime, CalendarEventSeries.EventRepeat eventRepeat) {

        switch (eventRepeat) {
            case DAILY:
                return nextDateTime.plusDays(1);
            case WEEKLY:
                return nextDateTime.with(TemporalAdjusters.next(nextDateTime.getDayOfWeek()));
            default:
                throw new GeneralApplicationException("Not all EventRepeat type is accommodated: " + eventRepeat.name());
        }
    }

    private CalendarEvent createRosterEvent(CalendarEvent calendarEvent) {
        return calendarEventService.saveCalendarEvent(calendarEvent);
    }
}
