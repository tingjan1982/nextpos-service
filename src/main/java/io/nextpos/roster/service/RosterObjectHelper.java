package io.nextpos.roster.service;

import io.nextpos.calendarevent.data.CalendarEvent;
import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.client.service.ClientService;
import io.nextpos.shared.auth.AuthenticationHelper;
import io.nextpos.shared.util.DateTimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class RosterObjectHelper {

    private final ClientService clientService;

    private final AuthenticationHelper authenticationHelper;

    @Autowired
    public RosterObjectHelper(ClientService clientService, AuthenticationHelper authenticationHelper) {
        this.clientService = clientService;
        this.authenticationHelper = authenticationHelper;
    }

    public CalendarEvent createRosterEvent(Client client, String eventName, LocalDateTime startTime, LocalDateTime endTime) {
        return new CalendarEvent(client.getId(),
                client.getZoneId(),
                CalendarEvent.EventType.ROSTER,
                eventName,
                CalendarEvent.EventOwner.createWithOwnerId(authenticationHelper.resolveCurrentUsername(), CalendarEvent.OwnerType.STAFF),
                DateTimeUtil.toDate(client.getZoneId(), startTime),
                DateTimeUtil.toDate(client.getZoneId(), endTime));
    }

    public List<CalendarEvent.EventResource> createRosterEventResources(Client client, Map<String, List<String>> workingAreaToUsernames) {

        return workingAreaToUsernames.entrySet().stream()
                .map(e -> e.getValue().stream()
                        .map(u -> {
                            final ClientUser clientUser = clientService.getClientUser(client, u);
                            return new CalendarEvent.EventResource(clientUser.getId().getUsername(),
                                    CalendarEvent.ResourceType.STAFF,
                                    clientUser.getName(),
                                    e.getKey());
                        }).collect(Collectors.toList()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }
}
