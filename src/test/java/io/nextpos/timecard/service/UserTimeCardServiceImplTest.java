package io.nextpos.timecard.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.client.service.ClientService;
import io.nextpos.shared.DummyObjects;
import io.nextpos.timecard.data.UserTimeCard;
import io.nextpos.timecard.data.UserTimeCardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This annotation runs the test without embedded mongodb:
 * https://stackoverflow.com/questions/52604062/how-to-disable-flapdoodle-embedded-mongodb-in-certain-tests
 *
 * \@TestPropertySource(properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration")
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
    private UserTimeCardRepository userTimeCardRepository;

    private Client client;


    @BeforeEach
    void prepare() {
        final ClientUser clientUser = DummyObjects.dummyClientUser();
        clientService.createClientUser(clientUser);

        client = clientService.saveClient(DummyObjects.dummyClient());
    }


    @Test
    void clockInAndOut() {

        final UserTimeCard userTimeCard = userTimeCardService.clockIn(client);

        assertThat(userTimeCard.getId()).isNotNull();
        assertThat(userTimeCard.getClockIn()).isNotNull().isBefore(LocalDateTime.now());
        assertThat(userTimeCard.getTimeCardStatus()).isEqualTo(UserTimeCard.TimeCardStatus.ACTIVE);

        userTimeCardService.getActiveTimeCard(client).orElseThrow();

        final UserTimeCard updatedUserTimeCard = userTimeCardService.clockOut(client);
        updatedUserTimeCard.setClockOut(LocalDateTime.now().plusMinutes(95));
        userTimeCardRepository.save(updatedUserTimeCard);

        assertThat(userTimeCardService.getUserTimeCardById(updatedUserTimeCard.getId())).isNotNull();

        assertThat(updatedUserTimeCard.getId()).isEqualTo(userTimeCard.getId());
        assertThat(updatedUserTimeCard.getClockOut()).isNotNull().isAfter(updatedUserTimeCard.getClockIn());
        assertThat(updatedUserTimeCard.getTimeCardStatus()).isEqualTo(UserTimeCard.TimeCardStatus.COMPLETE);
        assertThat(updatedUserTimeCard.getWorkingDuration().toHours()).isEqualTo(1);
        assertThat(updatedUserTimeCard.getWorkingDuration().toMinutesPart()).isEqualTo(35);
    }

    @Test
    void getUserTimeCardsByDateRange() {

        createUserTimeCard("user-1", LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        createUserTimeCard("user-1", LocalDateTime.now().minusDays(1), LocalDateTime.now().minusHours(6));
        createUserTimeCard("user-2", LocalDateTime.now().minusDays(1), LocalDateTime.now().minusHours(6));

        final List<UserTimeCard> userTimeCardsByDateRange = userTimeCardService.getUserTimeCardsByYearMonth(client, "user-1", YearMonth.now());

        LOGGER.info("{}", userTimeCardsByDateRange);

        assertThat(userTimeCardsByDateRange).hasSize(2);
        assertThat(userTimeCardsByDateRange).isSortedAccordingTo(Comparator.comparing(UserTimeCard::getClockIn));
    }

    void createUserTimeCard(String username, LocalDateTime clockIn, LocalDateTime clockOut) {

        UserTimeCard userTimeCard = new UserTimeCard(client.getId(), username, null);
        userTimeCard.setClockIn(clockIn);
        userTimeCard.setClockOut(clockOut);

        userTimeCardRepository.save(userTimeCard);
    }
}