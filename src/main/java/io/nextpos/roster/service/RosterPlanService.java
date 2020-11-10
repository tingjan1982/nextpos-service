package io.nextpos.roster.service;

import io.nextpos.calendarevent.data.CalendarEvent;
import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.roster.data.RosterPlan;

import java.time.YearMonth;
import java.util.List;

public interface RosterPlanService {

    RosterPlan saveRosterPlan(RosterPlan rosterPlan);

    RosterPlan getRosterPlan(String id);

    List<CalendarEvent> createCalendarEventsFromRosterPlan(Client client, RosterPlan rosterPlan);

    CalendarEvent assignStaffMember(CalendarEvent calendarEvent, ClientUser clientUser);

    CalendarEvent removeStaffMember(CalendarEvent calendarEvent);

    List<CalendarEvent> getCalendarEventsForStaffMember(Client client, ClientUser clientUser, YearMonth yearMonth);
}
