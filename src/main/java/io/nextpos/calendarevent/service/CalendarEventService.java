package io.nextpos.calendarevent.service;

import io.nextpos.calendarevent.data.CalendarEvent;
import io.nextpos.client.data.Client;

import java.time.YearMonth;
import java.util.List;

public interface CalendarEventService {

    CalendarEvent saveCalendarEvent(CalendarEvent calendarEvent);

    CalendarEvent updateEventResource(CalendarEvent calendarEvent, CalendarEvent.EventResource eventResource);

    CalendarEvent removeEventResource(CalendarEvent calendarEvent);

    List<CalendarEvent> getCalendarEventsForEventResource(Client client, YearMonth yearMonth, CalendarEvent.EventType eventType, CalendarEvent.EventResource eventResource);
}
