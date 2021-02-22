package io.nextpos.roster.service;

import io.nextpos.calendarevent.data.CalendarEvent;
import io.nextpos.calendarevent.service.CalendarEventService;
import io.nextpos.calendarevent.service.bean.EventRepeatObject;
import io.nextpos.calendarevent.service.bean.UpdateCalendarEventObject;
import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import io.nextpos.shared.util.DateTimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Date;
import java.util.List;

@Service
@ChainedTransaction
public class RosterPlanServiceImpl implements RosterPlanService {

    private final CalendarEventService calendarEventService;

    @Autowired
    public RosterPlanServiceImpl(CalendarEventService calendarEventService) {
        this.calendarEventService = calendarEventService;
    }

    @Override
    public List<CalendarEvent> createRosterEvent(CalendarEvent baseCalendarEvent, EventRepeatObject eventRepeatObject) {
        return calendarEventService.createCalendarEvent(baseCalendarEvent, eventRepeatObject);
    }

    @Override
    public List<CalendarEvent> getRosterEvents(Client client, YearMonth yearMonth) {

        final Date startOfMonth = DateTimeUtil.toDate(client.getZoneId(), yearMonth.atDay(1).atStartOfDay());
        final Date endOfMonth = DateTimeUtil.toDate(client.getZoneId(), yearMonth.atEndOfMonth().atTime(23, 59, 59));

        return calendarEventService.getCalendarEvents(client.getId(), CalendarEvent.EventType.ROSTER, startOfMonth, endOfMonth);
    }

    @Override
    public List<CalendarEvent> getTodaysClientUserRosterEvents(Client client, ClientUser clientUser) {

        final CalendarEvent.EventResource eventResource = toEventResource(clientUser);
        final Date from = Date.from(LocalDate.now().atStartOfDay().atZone(client.getZoneId()).toInstant());
        final Date to = Date.from(LocalDate.now().atTime(23, 59, 59).atZone(client.getZoneId()).toInstant());

        return calendarEventService.getCalendarEvents(client.getId(), CalendarEvent.EventType.ROSTER, eventResource, from, to);
    }

    @Override
    public CalendarEvent getRosterEvent(String id) {
        return calendarEventService.getCalendarEvent(id);
    }

    @Override
    public List<CalendarEvent> updateRosterEvent(CalendarEvent calendarEvent, UpdateCalendarEventObject updateRosterEvent) {
        return calendarEventService.updateCalendarEvent(calendarEvent, updateRosterEvent);
    }

    @Override
    public CalendarEvent updateSelfRosterEventResources(CalendarEvent calendarEvent, String username, List<CalendarEvent.EventResource> eventResources) {
        return calendarEventService.updateSelfEventResources(calendarEvent, username, eventResources);
    }

    @Override
    public List<CalendarEvent> updateRosterEventResources(CalendarEvent calendarEvent, List<CalendarEvent.EventResource> eventResources, boolean applyToSeries) {
        return calendarEventService.updateEventResources(calendarEvent, eventResources, applyToSeries);
    }

    @Override
    public void deleteRosterEvent(String id, boolean applyToSeries) {
        final CalendarEvent calendarEvent = calendarEventService.getCalendarEvent(id);
        calendarEventService.deleteCalendarEvent(calendarEvent, applyToSeries);
    }

    private CalendarEvent.EventResource toEventResource(ClientUser clientUser) {
        return new CalendarEvent.EventResource(clientUser.getId(), CalendarEvent.ResourceType.STAFF);
    }
}
