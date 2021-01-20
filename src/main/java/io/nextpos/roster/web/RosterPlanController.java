package io.nextpos.roster.web;

import io.nextpos.calendarevent.data.CalendarEvent;
import io.nextpos.calendarevent.service.CalendarEventService;
import io.nextpos.calendarevent.web.model.CalendarEventResponse;
import io.nextpos.calendarevent.web.model.CalendarEventsResponse;
import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.client.service.ClientService;
import io.nextpos.roster.data.RosterPlan;
import io.nextpos.roster.service.RosterPlanService;
import io.nextpos.roster.web.model.*;
import io.nextpos.shared.auth.OAuth2Helper;
import io.nextpos.shared.web.ClientResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Deprecated
@RestController
@RequestMapping("/rosterPlans")
public class RosterPlanController {

    private final RosterPlanService rosterPlanService;

    private final CalendarEventService calendarEventService;

    private final ClientService clientService;

    private final OAuth2Helper oAuth2Helper;

    @Autowired
    public RosterPlanController(RosterPlanService rosterPlanService, CalendarEventService calendarEventService, ClientService clientService, OAuth2Helper oAuth2Helper) {
        this.rosterPlanService = rosterPlanService;
        this.calendarEventService = calendarEventService;
        this.clientService = clientService;
        this.oAuth2Helper = oAuth2Helper;
    }

    @PostMapping
    public RosterPlanResponse createRosterPlan(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                               @Valid @RequestBody RosterPlanRequest request) {

        RosterPlan rosterPlan = fromRosterPlanRequest(client, request);

        return toResponse(rosterPlanService.saveRosterPlan(rosterPlan));
    }

    private RosterPlan fromRosterPlanRequest(Client client, RosterPlanRequest request) {
        final YearMonth rosterMonth = YearMonth.of(request.getYear(), request.getMonth());

        final RosterPlan rosterPlan = new RosterPlan(client.getId(), rosterMonth);
        request.getRosterEntries().forEach(entry -> rosterPlan.addRosterEntry(entry.getDayOfWeek(), entry.getStartTime(), entry.getEndTime()));

        return rosterPlan;
    }

    @GetMapping
    public RosterPlansResponse getRosterPlans(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        final List<RosterPlanResponse> results = rosterPlanService.getRosterPlans(client).stream()
                .map(RosterPlanResponse::new)
                .collect(Collectors.toList());

        return new RosterPlansResponse(results);
    }

    @GetMapping("/{id}")
    public RosterPlanResponse getRosterPlan(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                            @PathVariable String id) {

        final RosterPlan rosterPlan = rosterPlanService.getRosterPlan(id);
        return toResponse(rosterPlan);
    }

    @PostMapping("/{id}/entries")
    public RosterPlanResponse addRosterEntry(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                             @PathVariable String id,
                                             @Valid @RequestBody RosterEventRequest request) {

        final RosterPlan rosterPlan = rosterPlanService.getRosterPlan(id);
        //rosterPlan.addRosterEntry(request.getStartTime(), request.getEndTime());

        return toResponse(rosterPlanService.saveRosterPlan(rosterPlan));
    }

    private RosterPlanResponse toResponse(RosterPlan rosterPlan) {
        return new RosterPlanResponse(rosterPlan);
    }

    @DeleteMapping("/{id}/entries/{rosterEntryId}")
    public RosterPlanResponse deleteRosterEntry(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                @PathVariable String id,
                                                @PathVariable String rosterEntryId) {

        final RosterPlan rosterPlan = rosterPlanService.getRosterPlan(id);
        rosterPlan.removeRosterEntry(rosterEntryId);

        return toResponse(rosterPlanService.saveRosterPlan(rosterPlan));
    }

    @PostMapping("/{id}/events")
    public CalendarEventsResponse createRosterPlanEvents(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                         @PathVariable String id) {

        final RosterPlan rosterPlan = rosterPlanService.getRosterPlan(id);
        return new CalendarEventsResponse(rosterPlanService.createRosterPlanEvents(client, rosterPlan));
    }

    @GetMapping("/{id}/events")
    public CalendarEventsResponse getRosterPlanEvents(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                      @PathVariable String id) {

        final RosterPlan rosterPlan = rosterPlanService.getRosterPlan(id);
        return new CalendarEventsResponse(rosterPlanService.getRosterPlanEvents(rosterPlan));
    }

    @DeleteMapping("/{id}/events")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRosterPlanEvents(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                       @PathVariable String id) {

        final RosterPlan rosterPlan = rosterPlanService.getRosterPlan(id);
        rosterPlanService.deleteRosterPlanEvents(rosterPlan);
    }

    @GetMapping("/{id}/events/{eventId}")
    public CalendarEventResponse getRosterPlanEvent(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                    @PathVariable String eventId) {

        CalendarEvent calendarEvent = calendarEventService.getCalendarEvent(eventId);

        return new CalendarEventResponse(calendarEvent);
    }

    @PostMapping("/{id}/events/{eventId}/assign")
    public CalendarEventResponse assignCurrentStaffMember(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                          @PathVariable String eventId) {

        final ClientUser clientUser = oAuth2Helper.resolveCurrentClientUser(client);
        CalendarEvent calendarEvent = calendarEventService.getCalendarEvent(eventId);
        final CalendarEvent savedCalendarEvent = rosterPlanService.assignRosterPlanEventToStaffMember(calendarEvent, clientUser);

        return new CalendarEventResponse(savedCalendarEvent);
    }

    @PostMapping("/{id}/events/{eventId}/remove")
    public CalendarEventResponse removeCurrentStaffMember(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                          @PathVariable String eventId) {

        final ClientUser clientUser = oAuth2Helper.resolveCurrentClientUser(client);
        CalendarEvent calendarEvent = calendarEventService.getCalendarEvent(eventId);
        final CalendarEvent savedCalendarEvent = rosterPlanService.removeStaffMemberFromRosterPlanEvent(calendarEvent, clientUser);

        return new CalendarEventResponse(savedCalendarEvent);
    }

    @PostMapping("/{id}/events/{eventId}/resources")
    public CalendarEventResponse assignResources(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                 @PathVariable String eventId,
                                                 @RequestBody RosterUserRequest request) {

        final List<ClientUser> clientUsers = request.getUsernames().stream()
                .map(u -> clientService.getClientUser(client, u))
                .collect(Collectors.toList());

        CalendarEvent calendarEvent = calendarEventService.getCalendarEvent(eventId);
        final CalendarEvent savedCalendarEvent = rosterPlanService.updateRosterPlanEventStaffMembers(calendarEvent, clientUsers);

        return new CalendarEventResponse(savedCalendarEvent);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRosterPlan(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                 @PathVariable String id) {

        final RosterPlan rosterPlan = rosterPlanService.getRosterPlan(id);
        rosterPlanService.deleteRosterPlan(rosterPlan);
    }
}
