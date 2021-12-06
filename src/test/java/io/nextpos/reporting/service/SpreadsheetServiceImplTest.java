package io.nextpos.reporting.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.datetime.service.ZonedDateRangeBuilder;
import io.nextpos.reporting.data.DateParameterType;
import io.nextpos.shared.DummyObjects;
import io.nextpos.shared.util.DateTimeUtil;
import io.nextpos.timecard.data.UserTimeCard;
import io.nextpos.timecard.service.UserTimeCardService;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
@Transactional
class SpreadsheetServiceImplTest {

    private final UserTimeCardService userTimeCardService;

    private final SpreadsheetService spreadsheetService;

    private final ClientService clientService;

    @Autowired
    SpreadsheetServiceImplTest(UserTimeCardService userTimeCardService, SpreadsheetService spreadsheetService, ClientService clientService) {
        this.userTimeCardService = userTimeCardService;
        this.spreadsheetService = spreadsheetService;
        this.clientService = clientService;
    }

    @Test
    void sendWorkBookNotification() {

        Client client = clientService.saveClient(DummyObjects.dummyClient());
        String username = "user-1";

        UserTimeCard userTimeCard = new UserTimeCard(client.getId(), username, username);
        userTimeCard.setClockIn(DateTimeUtil.toDate(client.getZoneId(), LocalDateTime.now()));
        userTimeCard.setClockOut(DateTimeUtil.toDate(client.getZoneId(), LocalDateTime.now().plusHours(5)));
        userTimeCard.setActualWorkingHours(6);
        userTimeCard.setActualWorkingMinutes(30);
        userTimeCard.setTimeCardStatus(UserTimeCard.TimeCardStatus.COMPLETE);

        userTimeCardService.saveUserTimeCard(userTimeCard);

        final ZonedDateRange dateRange = ZonedDateRangeBuilder.builder(client, DateParameterType.TODAY).build();

        final List<UserTimeCard> userTimeCards = userTimeCardService.getUserTimeCardsByDateRange(client, dateRange);
        final Workbook wb = spreadsheetService.createUserTimeCardSpreadsheet(client, dateRange, userTimeCards);

        spreadsheetService.sendWorkBookNotification(client, "tingjan1982@gmail.com", dateRange, wb);
    }
}