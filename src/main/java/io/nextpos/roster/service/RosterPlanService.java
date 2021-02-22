package io.nextpos.roster.service;

import io.nextpos.calendarevent.data.CalendarEvent;
import io.nextpos.calendarevent.service.bean.EventRepeatObject;
import io.nextpos.calendarevent.service.bean.UpdateCalendarEventObject;
import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;

import java.time.YearMonth;
import java.util.List;

public interface RosterPlanService {

    List<CalendarEvent> createRosterEvent(CalendarEvent baseCalendarEvent, EventRepeatObject eventRepeatObject);

    List<CalendarEvent> getRosterEvents(Client client, YearMonth yearMonth);

    List<CalendarEvent> getTodaysClientUserRosterEvents(Client client, ClientUser clientUser);

    CalendarEvent getRosterEvent(String id);

    List<CalendarEvent> updateRosterEvent(CalendarEvent calendarEvent, UpdateCalendarEventObject updateRosterEvent);

    CalendarEvent updateSelfRosterEventResources(CalendarEvent calendarEvent, String username, List<CalendarEvent.EventResource> eventResources);

    List<CalendarEvent> updateRosterEventResources(CalendarEvent calendarEvent, List<CalendarEvent.EventResource> eventResources, boolean applyToSeries);

    void deleteRosterEvent(String id, boolean applyToSeries);
}
