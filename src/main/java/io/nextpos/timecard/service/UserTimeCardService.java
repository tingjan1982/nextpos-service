package io.nextpos.timecard.service;

import io.nextpos.client.data.Client;
import io.nextpos.timecard.data.UserTimeCard;

import java.time.YearMonth;
import java.util.List;

public interface UserTimeCardService {

    UserTimeCard clockIn(Client client);

    UserTimeCard clockOut(Client client);

    UserTimeCard getMostRecentTimeCard(Client client);

    UserTimeCard getUserTimeCardById(String id);

    List<UserTimeCard> getUserTimeCardsByYearMonth(Client client, String userId, YearMonth yearMonth);
}
