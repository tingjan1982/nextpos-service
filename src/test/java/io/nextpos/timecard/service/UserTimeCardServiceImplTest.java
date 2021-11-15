package io.nextpos.timecard.service;

import io.nextpos.calendarevent.data.CalendarEvent;
import io.nextpos.calendarevent.service.bean.EventRepeatObject;
import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.client.service.ClientService;
import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.datetime.service.ZonedDateRangeBuilder;
import io.nextpos.reporting.data.DateParameterType;
import io.nextpos.roster.service.RosterObjectHelper;
import io.nextpos.roster.service.RosterPlanService;
import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.util.DateTimeUtil;
import io.nextpos.timecard.data.UserTimeCard;
import io.nextpos.timecard.data.UserTimeCardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This annotation runs the test without embedded mongodb:
 * https://stackoverflow.com/questions/52604062/how-to-disable-flapdoodle-embedded-mongodb-in-certain-tests
 *
 * @TestPropertySource(properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration")
 */
@SpringBootTest
@Transactional
class UserTimeCardServiceImplTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserTimeCardServiceImplTest.class);

    @Autowired
    private UserTimeCardService userTimeCardService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private RosterPlanService rosterPlanService;

    @Autowired
    private RosterObjectHelper rosterObjectHelper;

    @Autowired
    private UserTimeCardRepository userTimeCardRepository;

    private Client client;


    @BeforeEach
    void prepare() {
        client = clientService.saveClient(DummyObjects.dummyClient());

        final ClientUser clientUser = DummyObjects.dummyClientUser(client);
        clientService.createClientUser(clientUser);

        CalendarEvent calendarEvent = rosterObjectHelper.createRosterEvent(client,
                "Morning",
                LocalDateTime.of(LocalDate.now(), LocalTime.of(8, 30)),
                LocalDateTime.of(LocalDate.now(), LocalTime.of(13, 30)));
        rosterPlanService.createRosterEvent(calendarEvent, EventRepeatObject.none());
        rosterPlanService.updateRosterEventResources(calendarEvent, rosterObjectHelper.createRosterEventResources(client, Map.of("bar", List.of("test-user"))), true);

        CalendarEvent calendarEvent2 = rosterObjectHelper.createRosterEvent(client,
                "Second morning",
                LocalDateTime.of(LocalDate.now(), LocalTime.of(9, 30)),
                LocalDateTime.of(LocalDate.now(), LocalTime.of(14, 30)));

        rosterPlanService.createRosterEvent(calendarEvent2, EventRepeatObject.none());
        rosterPlanService.updateRosterEventResources(calendarEvent2, rosterObjectHelper.createRosterEventResources(client, Map.of("bar", List.of("test-user"))), true);
    }

    @Test
    @WithMockUser("test-user")
    void clockInAndOut() {

        final UserTimeCard userTimeCard = userTimeCardService.clockIn(client);

        assertThat(userTimeCard.getId()).isNotNull();
        assertThat(userTimeCard.getUsername()).isEqualTo("test-user");
        assertThat(userTimeCard.getClockIn()).isNotNull().isBefore(new Date());
        assertThat(userTimeCard.getTimeCardStatus()).isEqualTo(UserTimeCard.TimeCardStatus.ACTIVE);
        assertThat(userTimeCard.getMatchedRoster()).isNotNull();

        assertThat(userTimeCardService.getMostRecentTimeCard(client)).isEqualTo(userTimeCard);

        final UserTimeCard updatedUserTimeCard = userTimeCardService.clockOut(client);
        updatedUserTimeCard.setClockOut(DateTimeUtil.toDate(ZoneId.systemDefault(), LocalDateTime.now().plusMinutes(95)));
        userTimeCardRepository.save(updatedUserTimeCard);

        assertThat(userTimeCardService.getUserTimeCardById(updatedUserTimeCard.getId())).isNotNull();

        assertThat(updatedUserTimeCard.getId()).isEqualTo(userTimeCard.getId());
        assertThat(updatedUserTimeCard.getClockOut()).isAfter(updatedUserTimeCard.getClockIn());
        assertThat(updatedUserTimeCard.getTimeCardStatus()).isEqualTo(UserTimeCard.TimeCardStatus.COMPLETE);
        assertThat(updatedUserTimeCard.getWorkingDuration().toHours()).isEqualTo(1);
        assertThat(updatedUserTimeCard.getWorkingDuration().toMinutesPart()).isEqualTo(35);
    }

    @Test
    void getUserTimeCardsByDateRange() {

        final LocalDateTime now = LocalDateTime.now();
        createUserTimeCard("user-2", now.withDayOfMonth(15), now.withDayOfMonth(15).plusHours(6));
        createUserTimeCard("user-1", now, now.plusDays(1));
        createUserTimeCard("user-1", now.withDayOfMonth(15), now.withDayOfMonth(15).plusHours(6));

        ZonedDateRange dateRange = ZonedDateRangeBuilder.builder(client, DateParameterType.MONTH).build();
        final List<UserTimeCard> timeCards = userTimeCardService.getUserTimeCardsByDateRange(client, dateRange);

        assertThat(timeCards).hasSize(3);

        final List<UserTimeCard> userTimeCardsByDateRange = userTimeCardService.getUserTimeCardsByYearMonth(client, "user-1", YearMonth.now());

        LOGGER.info("{}", userTimeCardsByDateRange);

        assertThat(userTimeCardsByDateRange).hasSize(2);
        assertThat(userTimeCardsByDateRange).isSortedAccordingTo(Comparator.comparing(UserTimeCard::getClockIn));
    }

    void createUserTimeCard(String username, LocalDateTime clockIn, LocalDateTime clockOut) {

        UserTimeCard userTimeCard = ObjectHelper.testTimeCard(client.getId(), username);
        userTimeCard.setClockIn(DateTimeUtil.toDate(client.getZoneId(), clockIn));
        userTimeCard.setClockOut(DateTimeUtil.toDate(client.getZoneId(), clockOut));

        userTimeCardRepository.save(userTimeCard);
    }
}