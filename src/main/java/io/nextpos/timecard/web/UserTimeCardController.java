package io.nextpos.timecard.web;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientObjectOwnershipService;
import io.nextpos.shared.web.ClientResolver;
import io.nextpos.timecard.data.UserTimeCard;
import io.nextpos.timecard.service.UserTimeCardService;
import io.nextpos.timecard.web.model.UserTimeCardResponse;
import io.nextpos.timecard.web.model.UserTimeCardsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/timecards")
public class UserTimeCardController {

    private final UserTimeCardService userTimeCardService;

    private final ClientObjectOwnershipService clientObjectOwnershipService;

    @Autowired
    public UserTimeCardController(final UserTimeCardService userTimeCardService, final ClientObjectOwnershipService clientObjectOwnershipService) {
        this.userTimeCardService = userTimeCardService;
        this.clientObjectOwnershipService = clientObjectOwnershipService;
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

    @GetMapping("/mostRecent")
    public UserTimeCardResponse getMostRecentUserTimeCard(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        final UserTimeCard userTimeCard = userTimeCardService.getMostRecentTimeCard(client);
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
                                                           @RequestParam(name = "userId", required = false) String userId,
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

    private UserTimeCardResponse toUserTimeCardResponse(UserTimeCard userTimeCard) {
        return new UserTimeCardResponse(userTimeCard);
    }
}
