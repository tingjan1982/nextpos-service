package io.nextpos.timecard.service;

import io.nextpos.client.data.Client;
import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.timecard.data.UserTimeCard;

import java.time.YearMonth;
import java.util.List;

public interface UserTimeCardService {

    UserTimeCard clockIn(Client client);

    UserTimeCard clockOut(Client client);

    UserTimeCard saveUserTimeCard(UserTimeCard userTimeCard);

    UserTimeCard getMostRecentTimeCard(Client client);

    UserTimeCard getUserTimeCardById(String id);

    List<UserTimeCard> getUserTimeCardsByDateRange(Client client, ZonedDateRange dateRange);

    List<UserTimeCard> getUserTimeCardsByYearMonth(Client client, String username, YearMonth yearMonth);
}
