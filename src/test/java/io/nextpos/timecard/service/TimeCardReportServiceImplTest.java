package io.nextpos.timecard.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.client.service.ClientService;
import io.nextpos.shared.DummyObjects;
import io.nextpos.timecard.data.TimeCardReport;
import io.nextpos.timecard.data.UserTimeCard;
import io.nextpos.timecard.data.UserTimeCardRepository;
import org.assertj.core.data.Index;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class TimeCardReportServiceImplTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeCardReportServiceImplTest.class);

    @Autowired
    private TimeCardReportService timeCardReportService;

    @Autowired
    private UserTimeCardRepository userTimeCardRepository;

    @Autowired
    private ClientService clientService;

    private Client client;

    @BeforeEach
    void setup() {
        client = clientService.saveClient(DummyObjects.dummyClient());
    }
    
    @Test
    void getTimeCardReport() {

        final String username = "user-1";
        final String username2 = "user-2";
        final Instant now = Instant.now();
        
        final Instant firstOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        final Instant lastOfMonth = YearMonth.now().atEndOfMonth().atStartOfDay(ZoneId.systemDefault()).toInstant();

        createUserTimeCard(username, Date.from(firstOfMonth), Date.from(firstOfMonth.plus(5, ChronoUnit.HOURS)));

        createUserTimeCard(username, Date.from(now), Date.from(now.plus(1, ChronoUnit.HALF_DAYS)));
        createUserTimeCard(username, Date.from(now), Date.from(now.plus(1, ChronoUnit.DAYS)));

        createUserTimeCard(username2, Date.from(now.plus(1, ChronoUnit.DAYS)), Date.from(now.plus(2, ChronoUnit.DAYS)));

        createUserTimeCard(username2, Date.from(lastOfMonth), Date.from(lastOfMonth.plus(1, ChronoUnit.DAYS)));

        final TimeCardReport result = timeCardReportService.getTimeCardReport(client, YearMonth.now());

        LOGGER.info("{}", result);

        assertThat(result.getUserTimeCards()).hasSize(2);
        assertThat(result.getUserTimeCards()).satisfies(u -> {
            assertThat(u.getTotalShifts()).isEqualTo(3);
            assertThat(u.getTotalHours()).isEqualByComparingTo("41.0");
        }, Index.atIndex(0));
        assertThat(result.getUserTimeCards()).satisfies(u -> {
            assertThat(u.getTotalShifts()).isEqualTo(2);
            assertThat(u.getTotalHours()).isEqualByComparingTo("48.0");
        }, Index.atIndex(1));
    }

    @Test
    void getTimeCardReport_EmptyResult() {

        final TimeCardReport result = timeCardReportService.getTimeCardReport(client, YearMonth.now().minus(1, ChronoUnit.MONTHS));

        assertThat(result.getUserTimeCards()).isEmpty();
    }

    @Test
    void getTimeCardReport_CheckEnhanceResult() {

        final Instant now = Instant.now();
        createUserTimeCard("zoe", Date.from(now), Date.from(now.plus(1, ChronoUnit.HALF_DAYS)));
        final ClientUser user1 = new ClientUser(new ClientUser.ClientUserId("sig", client.getUsername()), "1qaz2wsx", "USER");
        clientService.saveClientUser(user1);
        final ClientUser user2 = new ClientUser(new ClientUser.ClientUserId("ada", client.getUsername()), "1qaz2wsx", "USER");
        clientService.saveClientUser(user2);

        final TimeCardReport result = timeCardReportService.getTimeCardReport(client, YearMonth.now());

        LOGGER.info("{}", result.getUserTimeCards());
        assertThat(result.getUserTimeCards()).hasSize(3);
        assertThat(result.getUserTimeCards()).isSortedAccordingTo(Comparator.comparing(TimeCardReport.UserShift::getId));
    }

    void createUserTimeCard(String username, Date clockIn, Date clockOut) {

        UserTimeCard userTimeCard = new UserTimeCard(client.getId(), username, null);
        userTimeCard.setClockIn(clockIn);
        userTimeCard.setClockOut(clockOut);

        userTimeCardRepository.save(userTimeCard);
    }
}