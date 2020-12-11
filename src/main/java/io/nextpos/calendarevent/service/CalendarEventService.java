package io.nextpos.calendarevent.service;

import io.nextpos.calendarevent.data.CalendarEvent;
import io.nextpos.client.data.Client;

import java.util.Date;
import java.util.List;

public interface CalendarEventService {

    CalendarEvent saveCalendarEvent(CalendarEvent calendarEvent);

    CalendarEvent getCalendarEvent(String id);

    CalendarEvent addEventResource(CalendarEvent calendarEvent, CalendarEvent.EventResource eventResource);

    CalendarEvent removeEventResource(CalendarEvent calendarEvent, CalendarEvent.EventResource eventResource);

    CalendarEvent updateEventResources(CalendarEvent calendarEvent, List<CalendarEvent.EventResource> eventResources);

    List<CalendarEvent> getCalendarEventsForEventOwner(String clientId, String eventOwnerId, CalendarEvent.OwnerType ownerType);

    List<CalendarEvent> getCalendarEventsForEventResource(Client client, Date from, Date to, CalendarEvent.EventType eventType, CalendarEvent.EventResource eventResource);

    void deleteCalendarEvents(String clientId, String eventOwnerId, CalendarEvent.OwnerType ownerType);
}
