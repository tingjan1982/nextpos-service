package io.nextpos.roster.service;

import io.nextpos.calendarevent.data.CalendarEvent;
import io.nextpos.calendarevent.data.CalendarEventSeries;
import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.roster.data.RosterPlan;

import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

public interface RosterPlanService {

    RosterPlan saveRosterPlan(RosterPlan rosterPlan);

    RosterPlan getRosterPlan(String id);

    List<RosterPlan> getRosterPlans(Client client);

    void deleteRosterPlan(RosterPlan rosterPlan);

    List<CalendarEvent> createRosterEvent(Client client, CalendarEventSeries.EventRepeat eventRepeat, CalendarEvent baseCalendarEvent);

    List<CalendarEvent> getRosterEvents(Client client, YearMonth yearMonth);

    List<CalendarEvent> getTodaysClientUserRosterEvents(Client client, ClientUser clientUser);

    CalendarEvent getRosterEvent(String id);

    CalendarEvent updateRosterEvent(CalendarEvent calendarEvent, LocalTime startTime, LocalTime endTime, boolean applyToSeries);

    CalendarEvent updateRosterEventResources(CalendarEvent calendarEvent, List<CalendarEvent.EventResource> eventResources);

    void deleteRosterEvent(String id, boolean applyToSeries);

    List<CalendarEvent> createRosterPlanEvents(Client client, RosterPlan rosterPlan);

    List<CalendarEvent> getRosterPlanEvents(RosterPlan rosterPlan);

    void deleteRosterPlanEvents(RosterPlan rosterPlan);

    CalendarEvent assignRosterPlanEventToStaffMember(CalendarEvent calendarEvent, ClientUser clientUser);

    CalendarEvent removeStaffMemberFromRosterPlanEvent(CalendarEvent calendarEvent, ClientUser clientUser);

    CalendarEvent updateRosterPlanEventStaffMembers(CalendarEvent calendarEvent, List<ClientUser> clientUsers);
}
