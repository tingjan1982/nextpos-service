package io.nextpos.timecard.web;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.shared.auth.OAuth2Helper;
import io.nextpos.shared.web.ClientResolver;
import io.nextpos.timecard.data.UserTimeCard;
import io.nextpos.timecard.service.UserTimeCardService;
import io.nextpos.timecard.web.model.UserTimeCardResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/timecards")
public class UserTimeCardController {

    private final UserTimeCardService userTimeCardService;

    private final OAuth2Helper oAuth2Helper;

    @Autowired
    public UserTimeCardController(final UserTimeCardService userTimeCardService, final OAuth2Helper oAuth2Helper) {
        this.userTimeCardService = userTimeCardService;
        this.oAuth2Helper = oAuth2Helper;
    }

    @PostMapping("/clockin")
    public UserTimeCardResponse clockIn(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        final UserTimeCard userTimeCard = userTimeCardService.clockIn(client);
        return toUserTimeCardResponse(userTimeCard);
    }

    @PostMapping("/clockout")
    public UserTimeCardResponse clockOut(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        final UserTimeCard userTimeCard = userTimeCardService.clockOut(client);
        return toUserTimeCardResponse(userTimeCard);
    }

    @GetMapping("/active")
    public UserTimeCardResponse getActiveUserTimeCard(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        final UserTimeCard userTimeCard = userTimeCardService.getActiveTimeCard(client).orElseGet(() -> {
            final ClientUser clientUser = oAuth2Helper.resolveCurrentClientUser(client);
            final UserTimeCard card = new UserTimeCard(client.getId(), clientUser.getId().getUsername(), clientUser.getNickname());
            card.setTimeCardStatus(UserTimeCard.TimeCardStatus.INACTIVE);

            return card;
        });

        return toUserTimeCardResponse(userTimeCard);
    }

    private UserTimeCardResponse toUserTimeCardResponse(final UserTimeCard userTimeCard) {
        return new UserTimeCardResponse(
                userTimeCard.getId(),
                userTimeCard.getClientId(),
                userTimeCard.getUsername(),
                userTimeCard.getNickname(),
                userTimeCard.getClockIn(),
                userTimeCard.getClockOut(),
                userTimeCard.getTimeCardStatus());
    }
}
