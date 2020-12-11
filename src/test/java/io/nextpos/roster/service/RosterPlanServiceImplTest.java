package io.nextpos.roster.service;

import io.nextpos.calendarevent.data.CalendarEvent;
import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.client.service.ClientService;
import io.nextpos.roster.data.RosterPlan;
import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.exception.ObjectNotFoundException;
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

import static org.assertj.core.api.Assertions.*;

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

        clientUser = new ClientUser(new ClientUser.ClientUserId("joelin", client.getId()), client,"12341234", "USER");
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
        assertThat(rosterPlanService.getRosterPlans(client)).isNotEmpty();

        rosterPlanService.deleteRosterPlan(rosterPlan);

        assertThatThrownBy(() -> rosterPlanService.getRosterPlan(rosterPlan.getId())).isInstanceOf(ObjectNotFoundException.class);
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
        rosterPlanService.saveRosterPlan(rosterPlan);

        final List<CalendarEvent> events = rosterPlanService.createRosterPlanEvents(client, rosterPlan);

        assertThat(rosterPlanService.getRosterPlan(rosterPlan.getId()).getStatus()).isEqualByComparingTo(RosterPlan.RosterPlanStatus.LOCKED);

        final int eventCount = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()).getDayOfMonth();

        assertThat(events).hasSize(eventCount);

        events.forEach(e -> {
            final CalendarEvent updatedCalendarEvent = rosterPlanService.assignRosterPlanEventToStaffMember(e, clientUser);
            assertThat(updatedCalendarEvent.getEventResources()).allSatisfy(r -> {
                assertThat(r.getResourceId()).isNotNull();
                assertThat(r.getResourceType()).isEqualByComparingTo(CalendarEvent.ResourceType.STAFF);
                assertThat(r.getResourceName()).isNotNull();
            });
        });

        assertThat(rosterPlanService.getRosterPlanEvents(rosterPlan)).hasSize(eventCount);
        assertThat(rosterPlanService.getStaffMemberRoster(client, clientUser, YearMonth.now())).hasSize(eventCount);

        events.forEach(e -> {
            final CalendarEvent updatedCalendarEvent = rosterPlanService.removeStaffMemberFromRosterPlanEvent(e, clientUser);
            assertThat(updatedCalendarEvent.getEventResources()).isEmpty();
        });

        events.forEach(e -> {
            rosterPlanService.updateRosterPlanEventStaffMembers(e, List.of(clientUser, clientUser));
            assertThat(e.getEventResources()).hasSize(1);
        });

        rosterPlanService.deleteRosterPlanEvents(rosterPlan);

        assertThat(rosterPlanService.getRosterPlan(rosterPlan.getId()).getStatus()).isEqualByComparingTo(RosterPlan.RosterPlanStatus.ACTIVE);

        assertThat(rosterPlanService.getRosterPlanEvents(rosterPlan)).isEmpty();
    }
}