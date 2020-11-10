package io.nextpos.roster.service;

import io.nextpos.calendarevent.data.CalendarEvent;
import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.client.service.ClientService;
import io.nextpos.roster.data.RosterPlan;
import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@ChainedTransaction
class RosterPlanServiceImplTest {

    private final RosterPlanService rosterPlanService;

    private final ClientService clientService;

    private Client client;

    private ClientUser clientUser;

    @Autowired
    RosterPlanServiceImplTest(RosterPlanService rosterPlanService, ClientService clientService) {
        this.rosterPlanService = rosterPlanService;
        this.clientService = clientService;
    }

    @BeforeEach
    void prepare() {
        client = DummyObjects.dummyClient();
        client.setTimezone("UTC");
        clientService.saveClient(client);

        clientUser = new ClientUser(new ClientUser.ClientUserId("joelin", client.getId()), "12341234", "USER");
        clientService.saveClientUser(clientUser);
    }

    @Test
    void saveRosterPlan() {

        final RosterPlan rosterPlan = new RosterPlan(client.getId(), YearMonth.now());
        rosterPlan.addRosterEntry(DayOfWeek.MONDAY, LocalTime.now(), LocalTime.now().plusHours(4));
        rosterPlanService.saveRosterPlan(rosterPlan);

        assertThat(rosterPlan.getId()).isNotNull();
        assertThat(rosterPlan.getRosterMonth()).isNotNull();
        assertThat(rosterPlan.getClientId()).isEqualTo(client.getId());

        assertThatCode(() -> rosterPlanService.getRosterPlan(rosterPlan.getId())).doesNotThrowAnyException();
    }

    @Test
    void createCalendarEventFromRosterPlan() {

        final RosterPlan rosterPlan = new RosterPlan(client.getId(), YearMonth.now());
        rosterPlan.addRosterEntry(DayOfWeek.MONDAY, LocalTime.now(), LocalTime.now().plusHours(4));
        rosterPlan.addRosterEntry(DayOfWeek.TUESDAY, LocalTime.now(), LocalTime.now().plusHours(8));
        rosterPlan.addRosterEntry(DayOfWeek.WEDNESDAY, LocalTime.now(), LocalTime.now().plusHours(8));
        rosterPlan.addRosterEntry(DayOfWeek.THURSDAY, LocalTime.now(), LocalTime.now().plusHours(8));
        rosterPlan.addRosterEntry(DayOfWeek.FRIDAY, LocalTime.now(), LocalTime.now().plusHours(8));
        rosterPlan.addRosterEntry(DayOfWeek.SATURDAY, LocalTime.now(), LocalTime.now().plusHours(8));
        rosterPlan.addRosterEntry(DayOfWeek.SUNDAY, LocalTime.now(), LocalTime.now().plusHours(8));

        final List<CalendarEvent> events = rosterPlanService.createCalendarEventsFromRosterPlan(client, rosterPlan);

        assertThat(events).hasSize(LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()).getDayOfMonth());

        events.forEach(e -> {
            final CalendarEvent updatedCalendarEvent = rosterPlanService.assignStaffMember(e, clientUser);
            assertThat(updatedCalendarEvent.getEventResource()).satisfies(r -> {
                assertThat(r.getResourceId()).isNotNull();
                assertThat(r.getResourceType()).isEqualByComparingTo(CalendarEvent.ResourceType.STAFF);
                assertThat(r.getResourceName()).isNotNull();
            });
        });

        final List<CalendarEvent> retrievedEvents = rosterPlanService.getCalendarEventsForStaffMember(client, clientUser, YearMonth.now());

        assertThat(retrievedEvents).hasSize(LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()).getDayOfMonth());
    }
}