package io.nextpos.roster.service;

import io.nextpos.calendarevent.data.CalendarEvent;
import io.nextpos.calendarevent.data.CalendarEventSeries;
import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;

import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

public interface RosterPlanService {

    List<CalendarEvent> createRosterEvent(Client client, CalendarEventSeries.EventRepeat eventRepeat, CalendarEvent baseCalendarEvent);

    List<CalendarEvent> getRosterEvents(Client client, YearMonth yearMonth);

    List<CalendarEvent> getTodaysClientUserRosterEvents(Client client, ClientUser clientUser);

    CalendarEvent getRosterEvent(String id);

    CalendarEvent updateRosterEvent(CalendarEvent calendarEvent, LocalTime startTime, LocalTime endTime, long daysDiff, boolean applyToSeries);

    CalendarEvent updateRosterEventResources(CalendarEvent calendarEvent, List<CalendarEvent.EventResource> eventResources);

    void deleteRosterEvent(String id, boolean applyToSeries);
}
