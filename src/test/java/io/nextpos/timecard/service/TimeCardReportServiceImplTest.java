package io.nextpos.timecard.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.client.service.ClientService;
import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.util.DateTimeUtil;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;

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
        final LocalDateTime now = LocalDateTime.now();

        final LocalDateTime firstOfMonth = now.withDayOfMonth(1);
        final LocalDateTime lastOfMonth = YearMonth.now().atEndOfMonth().atStartOfDay();

        createUserTimeCard(username, firstOfMonth, firstOfMonth.plusHours(5));

        createUserTimeCard(username, now, now.plus(1, ChronoUnit.HALF_DAYS));
        createUserTimeCard(username, now, now.plusDays(1));

        createUserTimeCard(username2, now.withDayOfMonth(15), now.withDayOfMonth(16));

        createUserTimeCard(username2, lastOfMonth, lastOfMonth.plusDays(1));

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

        createUserTimeCard("zoe", LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        final ClientUser user1 = new ClientUser(client, "sig", "1qaz2wsx", "USER");
        clientService.saveClientUser(user1);
        final ClientUser user2 = new ClientUser(client, "ada", "1qaz2wsx", "USER");
        clientService.saveClientUser(user2);

        final TimeCardReport result = timeCardReportService.getTimeCardReport(client, YearMonth.now());

        LOGGER.info("{}", result.getUserTimeCards());
        assertThat(result.getUserTimeCards()).hasSize(3);
        assertThat(result.getUserTimeCards()).isSortedAccordingTo(Comparator.comparing(TimeCardReport.UserShift::getId));
    }

    void createUserTimeCard(String username, LocalDateTime clockIn, LocalDateTime clockOut) {

        UserTimeCard userTimeCard = new UserTimeCard(client.getId(), username, null);
        userTimeCard.setClockIn(DateTimeUtil.toDate(client.getZoneId(), clockIn));
        userTimeCard.setClockOut(DateTimeUtil.toDate(client.getZoneId(), clockOut));

        userTimeCardRepository.save(userTimeCard);
    }
}