package io.nextpos.roster.service;

import io.nextpos.calendarevent.data.CalendarEvent;
import io.nextpos.calendarevent.data.CalendarEventSeries;
import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

public interface RosterPlanService {

    List<CalendarEvent> createRosterEvent(Client client, CalendarEventSeries.EventRepeat eventRepeat, CalendarEvent baseCalendarEvent);

    List<CalendarEvent> getRosterEvents(Client client, YearMonth yearMonth);

    List<CalendarEvent> getTodaysClientUserRosterEvents(Client client, ClientUser clientUser);

    CalendarEvent getRosterEvent(String id);

    CalendarEvent updateRosterEvent(CalendarEvent calendarEvent, LocalDateTime startTime, LocalDateTime endTime, long daysDiff, boolean applyToSeries);

    CalendarEvent updateSelfRosterEventResources(CalendarEvent calendarEvent, String username, List<CalendarEvent.EventResource> eventResources);

    CalendarEvent updateRosterEventResources(CalendarEvent calendarEvent, List<CalendarEvent.EventResource> eventResources, boolean applyToSeries);

    void deleteRosterEvent(String id, boolean applyToSeries);
}
