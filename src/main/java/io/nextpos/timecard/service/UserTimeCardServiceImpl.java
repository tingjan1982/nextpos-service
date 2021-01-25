package io.nextpos.timecard.service;

import io.nextpos.calendarevent.data.CalendarEvent;
import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.client.service.ClientService;
import io.nextpos.roster.service.RosterPlanService;
import io.nextpos.shared.auth.AuthenticationHelper;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.service.annotation.MongoTransaction;
import io.nextpos.timecard.data.UserTimeCard;
import io.nextpos.timecard.data.UserTimeCardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
@MongoTransaction
public class UserTimeCardServiceImpl implements UserTimeCardService {

    private final UserTimeCardRepository userTimeCardRepository;

    private final ClientService clientService;

    private final RosterPlanService rosterPlanService;

    private final AuthenticationHelper authenticationHelper;

    @Autowired
    public UserTimeCardServiceImpl(UserTimeCardRepository userTimeCardRepository, ClientService clientService, RosterPlanService rosterPlanService, AuthenticationHelper authenticationHelper) {
        this.userTimeCardRepository = userTimeCardRepository;
        this.clientService = clientService;
        this.rosterPlanService = rosterPlanService;
        this.authenticationHelper = authenticationHelper;
    }

    @Override
    public UserTimeCard clockIn(final Client client) {

        final Optional<UserTimeCard> activeTimeCard = getActiveTimeCard(client);

        if (activeTimeCard.isPresent()) {
            return activeTimeCard.get();
        }

        final ClientUser clientUser = clientService.getCurrentClientUser(client);
        final UserTimeCard userTimeCard = new UserTimeCard(client.getId(), clientUser.getId().getUsername(), clientUser.getNickname());
        userTimeCard.clockIn();

        final List<CalendarEvent> clientUserRosters = rosterPlanService.getTodaysClientUserRosterEvents(client, clientUser);

        if (!CollectionUtils.isEmpty(clientUserRosters)) {
            long clockInMillis = userTimeCard.getClockIn().getTime();

            clientUserRosters.stream()
                    .min((e1, e2) -> {
                        final Long diff1 = Math.abs(e1.getStartTime().getTime() - clockInMillis);
                        final Long diff2 = Math.abs(e2.getStartTime().getTime() - clockInMillis);
                        return diff1.compareTo(diff2);
                    })
                    .ifPresent(userTimeCard::setMatchedRoster);
        }

        return userTimeCardRepository.save(userTimeCard);
    }

    @Override
    public UserTimeCard clockOut(final Client client) {

        final UserTimeCard userTimeCard = this.getActiveTimeCard(client).orElseThrow(() -> {
            throw new ObjectNotFoundException("UserTimeCard[ACTIVE]", UserTimeCard.class);
        });

        userTimeCard.clockOut();
        return userTimeCardRepository.save(userTimeCard);
    }

    private Optional<UserTimeCard> getActiveTimeCard(final Client client) {

        final String username = authenticationHelper.resolveCurrentUsername();
        return userTimeCardRepository.findByClientIdAndUsernameAndTimeCardStatus(client.getId(), username, UserTimeCard.TimeCardStatus.ACTIVE);
    }

    @Override
    public UserTimeCard getMostRecentTimeCard(final Client client) {

        final String username = authenticationHelper.resolveCurrentUsername();
        return userTimeCardRepository.findFirstByClientIdAndUsernameOrderByCreatedDateDesc(client.getId(), username).orElseGet(() -> {

            final ClientUser clientUser = clientService.getCurrentClientUser(client);
            final UserTimeCard card = new UserTimeCard(client.getId(), clientUser.getId().getUsername(), clientUser.getNickname());
            card.setTimeCardStatus(UserTimeCard.TimeCardStatus.INACTIVE);

            return card;
        });
    }

    @Override
    public UserTimeCard getUserTimeCardById(final String id) {
        return userTimeCardRepository.findById(id).orElseThrow(() -> {
            throw new ObjectNotFoundException(id, UserTimeCard.class);
        });
    }

    @Override
    public List<UserTimeCard> getUserTimeCardsByYearMonth(Client client, String username, YearMonth yearMonth) {

        return userTimeCardRepository.findAllByClientIdAndUsernameAndClockInDateRange(
                client.getId(),
                username,
                yearMonth.atDay(1),
                yearMonth.atEndOfMonth().plusDays(1),
                Sort.by(Sort.Direction.ASC, "clockIn")
        );
    }
}
