package io.nextpos.timecard.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.shared.auth.OAuth2Helper;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.timecard.data.UserTimeCard;
import io.nextpos.timecard.data.UserTimeCardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Optional;

@Service
@Transactional
public class UserTimeCardServiceImpl implements UserTimeCardService {

    private final UserTimeCardRepository userTimeCardRepository;

    private final OAuth2Helper oAuth2Helper;

    @Autowired
    public UserTimeCardServiceImpl(final UserTimeCardRepository userTimeCardRepository, final OAuth2Helper oAuth2Helper) {
        this.userTimeCardRepository = userTimeCardRepository;
        this.oAuth2Helper = oAuth2Helper;
    }

    @Override
    public UserTimeCard clockIn(final Client client) {

        final Optional<UserTimeCard> activeTimeCard = getActiveTimeCard(client);

        if (activeTimeCard.isPresent()) {
            return activeTimeCard.get();
        }

        final ClientUser clientUser = oAuth2Helper.resolveCurrentClientUser(client);
        final UserTimeCard userTimeCard = new UserTimeCard(client.getId(), clientUser.getId().getUsername(), clientUser.getNickname());
        userTimeCard.clockIn();
        
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

    @Override
    public Optional<UserTimeCard> getActiveTimeCard(final Client client) {
        final String username = oAuth2Helper.getCurrentPrincipal();
        return userTimeCardRepository.findByClientIdAndUsernameAndTimeCardStatus(client.getId(), username, UserTimeCard.TimeCardStatus.ACTIVE);
    }

    @Override
    public Optional<UserTimeCard> getMostRecentTimeCard(final Client client) {
        final String username = oAuth2Helper.getCurrentPrincipal();
        return userTimeCardRepository.findFirstByClientIdAndUsernameOrderByCreatedDateDesc(client.getId(), username);
    }
}
