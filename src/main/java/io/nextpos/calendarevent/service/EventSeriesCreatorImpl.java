package io.nextpos.calendarevent.service;

import io.nextpos.calendarevent.data.CalendarEvent;
import io.nextpos.calendarevent.data.CalendarEventRepository;
import io.nextpos.calendarevent.data.CalendarEventSeries;
import io.nextpos.calendarevent.data.CalendarEventSeriesRepository;
import io.nextpos.calendarevent.service.bean.EventRepeatObject;
import io.nextpos.shared.exception.GeneralApplicationException;
import io.nextpos.shared.util.DateTimeUtil;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Component
public class EventSeriesCreatorImpl implements EventSeriesCreator {

    private final CalendarEventRepository calendarEventRepository;

    private final CalendarEventSeriesRepository calendarEventSeriesRepository;

    public EventSeriesCreatorImpl(CalendarEventRepository calendarEventRepository, CalendarEventSeriesRepository calendarEventSeriesRepository) {
        this.calendarEventRepository = calendarEventRepository;
        this.calendarEventSeriesRepository = calendarEventSeriesRepository;
    }

    @Override
    public List<CalendarEvent> createEventSeriesEvent(CalendarEvent baseCalendarEvent, EventRepeatObject eventRepeatObj) {

        final ZoneId zoneId = baseCalendarEvent.getZoneId();
        CalendarEventSeries.EventRepeat eventRepeat = eventRepeatObj.getEventRepeat();
        LocalDateTime repeatEndDate = resolveRepeatEndDate(baseCalendarEvent, eventRepeatObj.getRepeatEndDate());

        final CalendarEventSeries calendarEventSeries = new CalendarEventSeries(baseCalendarEvent, eventRepeat, repeatEndDate);
        calendarEventSeriesRepository.save(calendarEventSeries);

        List<CalendarEvent> calendarEvents = new ArrayList<>();
        baseCalendarEvent.setEventSeries(calendarEventSeries);
        calendarEvents.add(this.saveCalendarEvent(baseCalendarEvent));

        LocalDateTime nextStartTime = DateTimeUtil.toLocalDateTime(zoneId, baseCalendarEvent.getStartTime());
        LocalDateTime nextEndTime = DateTimeUtil.toLocalDateTime(zoneId, baseCalendarEvent.getEndTime());
        LocalDateTime lastDayOfTheSeries = lastDayOfTheSeries(nextStartTime, eventRepeat);

        while (nextStartTime.compareTo(lastDayOfTheSeries) < 0 && nextStartTime.compareTo(repeatEndDate) < 0) {
            nextStartTime = resolveNextTime(nextStartTime, eventRepeat);
            nextEndTime = resolveNextTime(nextEndTime, eventRepeat);

            final CalendarEvent copiedCalendarEvent = baseCalendarEvent.copy(
                    DateTimeUtil.toDate(zoneId, nextStartTime),
                    DateTimeUtil.toDate(zoneId, nextEndTime));

            calendarEvents.add(this.saveCalendarEvent(copiedCalendarEvent));
        }

        return calendarEvents;
    }

    private LocalDateTime resolveRepeatEndDate(CalendarEvent calendarEvent, LocalDateTime repeatEndDate) {

        final LocalDateTime endOfMonth = DateTimeUtil.toLocalDate(calendarEvent.getZoneId(), calendarEvent.getStartTime()).with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59);

        if (repeatEndDate == null || repeatEndDate.compareTo(endOfMonth) > 0) {
            return endOfMonth;
        }

        final LocalDateTime startTime = DateTimeUtil.toLocalDateTime(calendarEvent.getZoneId(), calendarEvent.getStartTime());

        if (repeatEndDate.compareTo(startTime) < 0) {
            return startTime;
        }

        return repeatEndDate;
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

    private CalendarEvent saveCalendarEvent(CalendarEvent calendarEvent) {
        return calendarEventRepository.save(calendarEvent);
    }
}
