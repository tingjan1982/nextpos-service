package io.nextpos.calendarevent.service;

import io.nextpos.calendarevent.data.CalendarEvent;
import io.nextpos.calendarevent.data.CalendarEventSeries;

import java.time.LocalTime;
import java.util.Date;
import java.util.List;

public interface CalendarEventService {

    CalendarEvent saveCalendarEvent(CalendarEvent calendarEvent);

    CalendarEvent getCalendarEvent(String id);

    CalendarEvent addEventResource(CalendarEvent calendarEvent, CalendarEvent.EventResource eventResource);

    CalendarEvent removeEventResource(CalendarEvent calendarEvent, CalendarEvent.EventResource eventResource);

    CalendarEvent updateEventResources(CalendarEvent calendarEvent, List<CalendarEvent.EventResource> eventResources);

    List<CalendarEvent> getCalendarEvents(String clientId, CalendarEvent.EventType eventType, Date from, Date to);

    List<CalendarEvent> getCalendarEventsForEventOwner(String clientId, String eventOwnerId, CalendarEvent.OwnerType ownerType);

    List<CalendarEvent> getCalendarEvents(String clientId, CalendarEvent.EventType eventType, CalendarEvent.EventResource eventResource, Date from, Date to);

    void deleteCalendarEvents(String clientId, String eventOwnerId, CalendarEvent.OwnerType ownerType);

    CalendarEvent updateCalendarEvent(CalendarEvent calendarEvent, LocalTime startTime, LocalTime endTime, long daysDiff, boolean applyToSeries);

    void deleteCalendarEvent(CalendarEvent calendarEvent, boolean applyToSeries);

    CalendarEventSeries saveCalendarEventSeries(CalendarEventSeries calendarEventSeries);
}
