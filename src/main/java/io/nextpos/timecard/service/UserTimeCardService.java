package io.nextpos.timecard.service;

import io.nextpos.client.data.Client;
import io.nextpos.timecard.data.UserTimeCard;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface UserTimeCardService {

    UserTimeCard clockIn(Client client);

    UserTimeCard clockOut(Client client);

    Optional<UserTimeCard> getActiveTimeCard(Client client);

    Optional<UserTimeCard> getMostRecentTimeCard(Client client);

    List<UserTimeCard> getUserTimeCardsByYearMonth(Client client, String username, YearMonth yearMonth);
}
