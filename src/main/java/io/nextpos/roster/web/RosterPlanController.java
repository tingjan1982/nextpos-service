package io.nextpos.roster.web;

import io.nextpos.calendarevent.data.CalendarEvent;
import io.nextpos.calendarevent.service.CalendarEventService;
import io.nextpos.calendarevent.web.model.CalendarEventResponse;
import io.nextpos.calendarevent.web.model.CalendarEventsResponse;
import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.roster.data.RosterPlan;
import io.nextpos.roster.service.RosterPlanService;
import io.nextpos.roster.web.model.RosterEntryRequest;
import io.nextpos.roster.web.model.RosterPlanRequest;
import io.nextpos.roster.web.model.RosterPlanResponse;
import io.nextpos.shared.auth.OAuth2Helper;
import io.nextpos.shared.web.ClientResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/rosterPlans")
public class RosterPlanController {

    private final RosterPlanService rosterPlanService;

    private final CalendarEventService calendarEventService;

    private final OAuth2Helper oAuth2Helper;

    @Autowired
    public RosterPlanController(RosterPlanService rosterPlanService, CalendarEventService calendarEventService, OAuth2Helper oAuth2Helper) {
        this.rosterPlanService = rosterPlanService;
        this.calendarEventService = calendarEventService;
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

    @GetMapping("/{id}")
    public RosterPlanResponse getRosterPlan(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                            @PathVariable String id) {

        final RosterPlan rosterPlan = rosterPlanService.getRosterPlan(id);
        return toResponse(rosterPlan);
    }

    @PostMapping("/{id}/entries")
    public RosterPlanResponse addRosterEntry(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                             @PathVariable String id,
                                             @Valid @RequestBody RosterEntryRequest request) {

        final RosterPlan rosterPlan = rosterPlanService.getRosterPlan(id);
        rosterPlan.addRosterEntry(request.getDayOfWeek(), request.getStartTime(), request.getEndTime());

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
        final List<CalendarEventResponse> results = rosterPlanService.createRosterPlanEvents(client, rosterPlan).stream()
                .map(CalendarEventResponse::new)
                .collect(Collectors.toList());

        return new CalendarEventsResponse(results);
    }

    @GetMapping("/{id}/events")
    public CalendarEventsResponse getRosterPlanEvents(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                      @PathVariable String id) {

        final RosterPlan rosterPlan = rosterPlanService.getRosterPlan(id);
        final List<CalendarEventResponse> results = rosterPlanService.getRosterPlanEvents(rosterPlan).stream()
                .map(CalendarEventResponse::new)
                .collect(Collectors.toList());

        return new CalendarEventsResponse(results);
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

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRosterPlan(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                 @PathVariable String id) {

        final RosterPlan rosterPlan = rosterPlanService.getRosterPlan(id);
        rosterPlanService.deleteRosterPlan(rosterPlan);
    }
}
