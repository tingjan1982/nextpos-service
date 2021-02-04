package io.nextpos.roster.service;

import io.nextpos.calendarevent.data.CalendarEvent;
import io.nextpos.calendarevent.data.CalendarEventSeries;
import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.client.service.ClientService;
import io.nextpos.roster.service.bean.EventRepeatObject;
import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ChainedTransaction
class RosterPlanServiceImplTest {

    private final RosterPlanService rosterPlanService;

    private final RosterObjectHelper rosterObjectHelper;

    private final ClientService clientService;

    private Client client;

    private ClientUser clientUser;

    @Autowired
    RosterPlanServiceImplTest(RosterPlanService rosterPlanService, RosterObjectHelper rosterObjectHelper, ClientService clientService) {
        this.rosterPlanService = rosterPlanService;
        this.rosterObjectHelper = rosterObjectHelper;
        this.clientService = clientService;
    }

    @BeforeEach
    void prepare() {
        client = DummyObjects.dummyClient();
        client.setTimezone("UTC");
        clientService.saveClient(client);

        clientUser = new ClientUser(new ClientUser.ClientUserId("joe", client.getUsername()), client,"12341234", "USER");
        clientService.saveClientUser(clientUser);

        ClientUser lin = new ClientUser(new ClientUser.ClientUserId("lin", client.getUsername()), client,"12341234", "USER");
        clientService.saveClientUser(lin);
    }

    @Test
    @WithMockUser
    void crudRosterEvent() {

        final CalendarEvent calendarEvent = rosterObjectHelper.createRosterEvent(client, "Morning shift", LocalDateTime.now(), LocalDateTime.now().plusHours(8));
        final List<CalendarEvent> createdRosterEvents = rosterPlanService.createRosterEvent(client, calendarEvent, EventRepeatObject.eventRepeat(CalendarEventSeries.EventRepeat.WEEKLY));

        assertThat(createdRosterEvents).isNotEmpty();
        assertThat(createdRosterEvents).allSatisfy(e -> {
            assertThat(e.getEventSeries()).isNotNull();
            assertThat(e.getStartTime()).isBefore(e.getEndTime());
        });

        assertThat(rosterPlanService.getRosterEvent(calendarEvent.getId())).satisfies(e -> {
            assertThat(e.getEventName()).isEqualTo("Morning shift");
            assertThat(e.getEventOwner()).satisfies(o -> {
                assertThat(o.getOwnerId()).isNotNull();
                assertThat(o.getOwnerType()).isEqualByComparingTo(CalendarEvent.OwnerType.STAFF);
                assertThat(o.getOwnerType()).isNotNull();
            });

            assertThat(e.getEventResources()).isEmpty();
        });

        calendarEvent.setEventName("Noon shift");
        rosterPlanService.updateRosterEvent(calendarEvent, LocalDateTime.of(LocalDate.now(), LocalTime.of(10, 30)), LocalDateTime.of(LocalDate.now(), LocalTime.of(3, 30)), 2, true);

        final List<CalendarEvent> rosterEvents = rosterPlanService.getRosterEvents(client, YearMonth.now());
        assertThat(rosterEvents).isNotEmpty();
        assertThat(rosterEvents).allSatisfy(e -> {
            assertThat(e.getEventName()).isEqualTo("Noon shift");
        });

        Map<String, List<String>> workingAreaToUsernames = Map.of("bar", List.of("joe", "lin"));
        final CalendarEvent updatedRosterEvent = rosterPlanService.updateRosterEventResources(calendarEvent, rosterObjectHelper.createRosterEventResources(client, workingAreaToUsernames), true);

        assertThat(updatedRosterEvent.getEventResources()).hasSize(2);

        final List<CalendarEvent> clientUserEvents = rosterPlanService.getTodaysClientUserRosterEvents(client, clientUser);
        assertThat(clientUserEvents).hasSize(1);

        rosterPlanService.deleteRosterEvent(calendarEvent.getId(), true);

        assertThat(rosterPlanService.getRosterEvents(client, YearMonth.now())).isEmpty();
    }

    @Test
    @WithMockUser
    void seriesEvent() {

        final CalendarEvent calendarEvent = rosterObjectHelper.createRosterEvent(client, "Morning shift", LocalDateTime.now(), LocalDateTime.now().plusHours(8));
        final List<CalendarEvent> createdRosterEvents = rosterPlanService.createRosterEvent(client,
                calendarEvent,
                new EventRepeatObject(CalendarEventSeries.EventRepeat.DAILY, null));

        final int days = Period.between(LocalDate.now(), LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()).plusDays(1)).getDays();

        assertThat(createdRosterEvents).hasSize(days);

        final LocalDate endOfWeek = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        final List<CalendarEvent> createdRosterEvents2 = rosterPlanService.createRosterEvent(client,
                calendarEvent,
                new EventRepeatObject(CalendarEventSeries.EventRepeat.DAILY, endOfWeek.atStartOfDay()));

        final int days2 = Period.between(LocalDate.now(), endOfWeek.plusDays(1)).getDays();

        assertThat(createdRosterEvents2).hasSize(days2);
    }
}