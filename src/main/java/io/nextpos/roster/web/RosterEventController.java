package io.nextpos.roster.web;

import io.nextpos.calendarevent.data.CalendarEvent;
import io.nextpos.calendarevent.web.model.CalendarEventResponse;
import io.nextpos.calendarevent.web.model.CalendarEventsResponse;
import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.client.service.ClientService;
import io.nextpos.roster.service.RosterObjectHelper;
import io.nextpos.roster.service.RosterPlanService;
import io.nextpos.roster.service.bean.EventRepeatObject;
import io.nextpos.roster.web.model.RosterEventRequest;
import io.nextpos.roster.web.model.RosterResourceRequest;
import io.nextpos.roster.web.model.UpdateRosterEventRequest;
import io.nextpos.shared.auth.AuthenticationHelper;
import io.nextpos.shared.util.DateTimeUtil;
import io.nextpos.shared.web.ClientResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/rosterEvents")
public class RosterEventController {

    private final RosterPlanService rosterPlanService;

    private final ClientService clientService;

    private final AuthenticationHelper authenticationHelper;

    private final RosterObjectHelper rosterObjectHelper;

    @Autowired
    public RosterEventController(RosterPlanService rosterPlanService, ClientService clientService, AuthenticationHelper authenticationHelper, RosterObjectHelper rosterObjectHelper) {
        this.rosterPlanService = rosterPlanService;
        this.clientService = clientService;
        this.authenticationHelper = authenticationHelper;
        this.rosterObjectHelper = rosterObjectHelper;
    }

    @PostMapping
    public CalendarEventsResponse createRosterEvent(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                    @Valid @RequestBody RosterEventRequest request) {

        final CalendarEvent calendarEvent = fromRosterEntryRequest(client, request);
        final List<CalendarEvent> rosterEventSeries = rosterPlanService.createRosterEvent(client, calendarEvent, new EventRepeatObject(request.getEventRepeat(), request.getRepeatEndDate()));

        return toResponse(client, rosterEventSeries);
    }

    private CalendarEvent fromRosterEntryRequest(Client client, RosterEventRequest request) {

        final CalendarEvent rosterEvent = rosterObjectHelper.createRosterEvent(client, request.getEventName(), request.getStartTime(), request.getEndTime());
        rosterEvent.setEventColor(request.getEventColor());

        return rosterEvent;
    }

    @GetMapping
    public CalendarEventsResponse getRosterEvents(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                  @RequestParam(name = "year") Integer year,
                                                  @RequestParam(name = "month") Integer month) {

        final List<CalendarEvent> rosterEvents = rosterPlanService.getRosterEvents(client, YearMonth.of(year, month));
        return toResponse(client, rosterEvents);
    }

    @GetMapping("/me")
    public CalendarEventsResponse getMyRostersToday(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        final ClientUser currentClientUser = clientService.getCurrentClientUser(client);
        final List<CalendarEvent> rosterEvents = rosterPlanService.getTodaysClientUserRosterEvents(client, currentClientUser);

        return toResponse(client, rosterEvents);
    }

    @GetMapping("/{id}")
    public CalendarEventResponse getRosterEventById(@PathVariable String id) {

        final CalendarEvent rosterEvent = rosterPlanService.getRosterEvent(id);
        return new CalendarEventResponse(rosterEvent);
    }

    @PostMapping("/{id}")
    public CalendarEventResponse updateRosterEvent(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                   @PathVariable String id,
                                                   @Valid @RequestBody UpdateRosterEventRequest request) {

        final CalendarEvent rosterEvent = rosterPlanService.getRosterEvent(id);

        final long daysDiff = ChronoUnit.DAYS.between(rosterEvent.getStartTime().toInstant(), request.getStartTime().atZone(client.getZoneId()).toInstant());

        updateFromRequest(client, rosterEvent, request);

        final CalendarEvent updatedCalendarEvent = rosterPlanService.updateRosterEvent(rosterEvent,
                request.getStartTime(),
                request.getEndTime(),
                daysDiff,
                request.isApplyToSeries());

        if (request.getWorkingAreaToUsernames() != null) {
            final List<CalendarEvent.EventResource> rosterEventResources = rosterObjectHelper.createRosterEventResources(client, request.getWorkingAreaToUsernames());
            rosterPlanService.updateRosterEventResources(updatedCalendarEvent, rosterEventResources, request.isApplyToSeries());
        }

        return new CalendarEventResponse(updatedCalendarEvent);
    }

    private void updateFromRequest(Client client, CalendarEvent rosterEvent, UpdateRosterEventRequest request) {

        rosterEvent.setEventName(request.getEventName());
        rosterEvent.setStartTime(DateTimeUtil.toDate(client.getZoneId(), request.getStartTime()));
        rosterEvent.setEndTime(DateTimeUtil.toDate(client.getZoneId(), request.getEndTime()));
        rosterEvent.setEventColor(request.getEventColor());
    }

    @PostMapping("/{id}/resources")
    public CalendarEventResponse assignEventResources(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                      @PathVariable String id,
                                                      @Valid @RequestBody RosterResourceRequest request) {

        final CalendarEvent rosterEvent = rosterPlanService.getRosterEvent(id);
        final String username = authenticationHelper.resolveCurrentUsername();
        final List<CalendarEvent.EventResource> eventResources = fromRosterResourceRequest(client, username, request);

        final CalendarEvent updatedCalendarEvent = rosterPlanService.updateSelfRosterEventResources(rosterEvent, username, eventResources);
        return new CalendarEventResponse(updatedCalendarEvent);
    }

    private List<CalendarEvent.EventResource> fromRosterResourceRequest(Client client, String username, RosterResourceRequest request) {

        final Map<String, List<String>> workingAreaToUsernames = request.getWorkingAreas().stream()
                .map(wa -> Map.entry(wa, List.of(username)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return rosterObjectHelper.createRosterEventResources(client, workingAreaToUsernames);
    }

    private CalendarEventsResponse toResponse(Client client, List<CalendarEvent> rosterEvents) {
        return new CalendarEventsResponse(client.getZoneId(), rosterEvents);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRosterEvent(@PathVariable String id) {

        rosterPlanService.deleteRosterEvent(id, false);
    }

    @DeleteMapping("/{id}/all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRosterEventSeries(@PathVariable String id) {

        rosterPlanService.deleteRosterEvent(id, true);
    }
}
