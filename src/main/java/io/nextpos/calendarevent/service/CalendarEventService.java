package io.nextpos.calendarevent.service;

import io.nextpos.calendarevent.data.CalendarEvent;
import io.nextpos.calendarevent.service.bean.EventRepeatObject;
import io.nextpos.calendarevent.service.bean.UpdateCalendarEventObject;

import java.util.Date;
import java.util.List;

public interface CalendarEventService {

    List<CalendarEvent> createCalendarEvent(CalendarEvent baseCalendarEvent, EventRepeatObject eventRepeatObj);

    CalendarEvent saveCalendarEvent(CalendarEvent calendarEvent);

    CalendarEvent getCalendarEvent(String id);

    CalendarEvent updateSelfEventResources(CalendarEvent calendarEvent, String resourceId, List<CalendarEvent.EventResource> eventResources);

    List<CalendarEvent> updateEventResources(CalendarEvent calendarEvent, List<CalendarEvent.EventResource> eventResources, boolean applyToSeries);

    List<CalendarEvent> getCalendarEvents(String clientId, CalendarEvent.EventType eventType, Date from, Date to);

    List<CalendarEvent> getCalendarEvents(String clientId, CalendarEvent.EventType eventType, CalendarEvent.EventResource eventResource, Date from, Date to);

    List<CalendarEvent> updateCalendarEvent(CalendarEvent calendarEvent, UpdateCalendarEventObject updateCalendarEvent);

    void deleteCalendarEvent(CalendarEvent calendarEvent, boolean applyToSeries);
}
