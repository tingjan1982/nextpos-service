package io.nextpos.calendarevent.service;

import io.nextpos.calendarevent.data.CalendarEvent;
import io.nextpos.calendarevent.data.CalendarEventSeries;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

public interface CalendarEventService {

    CalendarEvent saveCalendarEvent(CalendarEvent calendarEvent);

    CalendarEvent getCalendarEvent(String id);

    CalendarEvent updateSelfEventResources(CalendarEvent calendarEvent, String resourceId, List<CalendarEvent.EventResource> eventResources);

    CalendarEvent updateEventResources(CalendarEvent calendarEvent, List<CalendarEvent.EventResource> eventResources, boolean applyToSeries);

    List<CalendarEvent> getCalendarEvents(String clientId, CalendarEvent.EventType eventType, Date from, Date to);

    List<CalendarEvent> getCalendarEvents(String clientId, CalendarEvent.EventType eventType, CalendarEvent.EventResource eventResource, Date from, Date to);

    CalendarEvent updateCalendarEvent(CalendarEvent calendarEvent, LocalDateTime startTime, LocalDateTime endTime, long daysDiff, boolean applyToSeries);

    void deleteCalendarEvent(CalendarEvent calendarEvent, boolean applyToSeries);

    CalendarEventSeries saveCalendarEventSeries(CalendarEventSeries calendarEventSeries);

}
