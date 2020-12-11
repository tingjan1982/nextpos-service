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

    List<RosterPlan> getRosterPlans(Client client);

    void deleteRosterPlan(RosterPlan rosterPlan);

    List<CalendarEvent> createRosterPlanEvents(Client client, RosterPlan rosterPlan);

    List<CalendarEvent> getRosterPlanEvents(RosterPlan rosterPlan);

    void deleteRosterPlanEvents(RosterPlan rosterPlan);

    CalendarEvent assignRosterPlanEventToStaffMember(CalendarEvent calendarEvent, ClientUser clientUser);

    CalendarEvent removeStaffMemberFromRosterPlanEvent(CalendarEvent calendarEvent, ClientUser clientUser);

    CalendarEvent updateRosterPlanEventStaffMembers(CalendarEvent calendarEvent, List<ClientUser> clientUsers);

    List<CalendarEvent> getStaffMemberRoster(Client client, ClientUser clientUser, YearMonth yearMonth);

}
