package io.nextpos.timecard.web;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.client.service.ClientObjectOwnershipService;
import io.nextpos.shared.auth.OAuth2Helper;
import io.nextpos.shared.web.ClientResolver;
import io.nextpos.timecard.data.UserTimeCard;
import io.nextpos.timecard.service.UserTimeCardService;
import io.nextpos.timecard.web.model.UserTimeCardResponse;
import io.nextpos.timecard.web.model.UserTimeCardsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/timecards")
public class UserTimeCardController {

    private final UserTimeCardService userTimeCardService;

    private final ClientObjectOwnershipService clientObjectOwnershipService;

    private final OAuth2Helper oAuth2Helper;

    @Autowired
    public UserTimeCardController(final UserTimeCardService userTimeCardService, final ClientObjectOwnershipService clientObjectOwnershipService, final OAuth2Helper oAuth2Helper) {
        this.userTimeCardService = userTimeCardService;
        this.clientObjectOwnershipService = clientObjectOwnershipService;
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

    @GetMapping("/mostRecent")
    public UserTimeCardResponse getMostRecentUserTimeCard(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        final UserTimeCard userTimeCard = userTimeCardService.getMostRecentTimeCard(client).orElseGet(() -> {
            final ClientUser clientUser = oAuth2Helper.resolveCurrentClientUser(client);
            final UserTimeCard card = new UserTimeCard(client.getId(), clientUser.getId().getUsername(), clientUser.getNickname());
            card.setTimeCardStatus(UserTimeCard.TimeCardStatus.INACTIVE);

            return card;
        });

        return toUserTimeCardResponse(userTimeCard);
    }

    @GetMapping("/{id}")
    public UserTimeCardResponse getUserTimeCardById(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                    @PathVariable final String id) {

        final UserTimeCard userTimeCard = clientObjectOwnershipService.checkWithClientIdOwnership(client, () -> userTimeCardService.getUserTimeCardById(id));
        return toUserTimeCardResponse(userTimeCard);
    }

    @GetMapping
    public UserTimeCardsResponse getUserTimeCardByUsername(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                           @RequestParam(name = "username") String username,
                                                           @RequestParam(name = "year", required = false) Integer year,
                                                           @RequestParam(name = "month", required = false) Integer month) {

        YearMonth yearMonth = YearMonth.now();

        if (year != null && month != null) {
            yearMonth = YearMonth.of(year, month);
        }

        final List<UserTimeCardResponse> userTimeCards = userTimeCardService.getUserTimeCardsByYearMonth(client, username, yearMonth).stream()
                .map(this::toUserTimeCardResponse)
                .collect(Collectors.toList());

        return new UserTimeCardsResponse(userTimeCards);
    }

    private UserTimeCardResponse toUserTimeCardResponse(final UserTimeCard userTimeCard) {
        final Duration workingDuration = userTimeCard.getWorkingDuration();

        return new UserTimeCardResponse(
                userTimeCard.getId(),
                userTimeCard.getClientId(),
                userTimeCard.getUsername(),
                userTimeCard.getNickname(),
                userTimeCard.getClockIn() != null ? Date.from(userTimeCard.getClockIn().atZone(ZoneId.systemDefault()).toInstant()) : null,
                userTimeCard.getClockOut() != null ? Date.from(userTimeCard.getClockOut().atZone(ZoneId.systemDefault()).toInstant()) : null,
                workingDuration.toHours(),
                workingDuration.toMinutesPart(),
                userTimeCard.getTimeCardStatus());
    }
}
