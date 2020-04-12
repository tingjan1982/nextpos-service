package io.nextpos.ordermanagement.web;

import io.nextpos.client.data.Client;
import io.nextpos.ordermanagement.data.Shift;
import io.nextpos.ordermanagement.service.ShiftService;
import io.nextpos.ordermanagement.web.model.CloseShiftRequest;
import io.nextpos.ordermanagement.web.model.OpenShiftRequest;
import io.nextpos.ordermanagement.web.model.ShiftResponse;
import io.nextpos.ordermanagement.web.model.ShiftsResponse;
import io.nextpos.shared.web.ClientResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/shifts")
public class ShiftController {

    private final ShiftService shiftService;

    @Autowired
    public ShiftController(final ShiftService shiftService) {
        this.shiftService = shiftService;
    }

    @GetMapping("/active")
    public ShiftResponse getActiveShift(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        final Shift shift = shiftService.getActiveShift(client.getId()).orElseGet(() -> {
            final Shift s = new Shift(client.getId(), null, null, null);
            s.setShiftStatus(Shift.ShiftStatus.INACTIVE);

            return s;
        });

        return toShiftResponse(shift);
    }

    @GetMapping("/mostRecent")
    public ShiftResponse getMostRecentShift(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        final Shift mostRecentShift = shiftService.getMostRecentShift(client.getId()).orElseGet(() -> {
            final Shift s = new Shift(client.getId(), null, null, null);
            s.setShiftStatus(Shift.ShiftStatus.INACTIVE);

            return s;
        });

        return toShiftResponse(mostRecentShift);
    }

    @GetMapping()
    public ShiftsResponse getShifts(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                    @RequestParam(value = "date", required = false) Date date) {

        Date searchDate = date;

        if (searchDate == null) {
            final Instant instant = LocalDateTime.now().minusDays(30).atZone(ZoneId.systemDefault()).toInstant();
            searchDate = Date.from(instant);
        }

        final PageRequest pageRequest = PageRequest.of(0, 30, Sort.by(Sort.Order.desc("start.timestamp")));

        final List<ShiftResponse> shifts = shiftService.getShifts(client.getId(), searchDate, pageRequest).stream()
                .map(this::toShiftResponse).collect(Collectors.toList());

        return new ShiftsResponse(shifts);
    }


    @PostMapping("/open")
    public ShiftResponse openShift(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                   @Valid @RequestBody OpenShiftRequest shiftRequest) {

        final Shift shift = shiftService.openShift(client.getId(), shiftRequest.getBalance());
        return toShiftResponse(shift);
    }

    @PostMapping("/initiateClose")
    public ShiftResponse initiateCloseShift(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        final Shift shift = shiftService.initiateCloseShift(client.getId());
        return toShiftResponse(shift);
    }

    @PostMapping("/close")
    public ShiftResponse closeShift(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                    @Valid @RequestBody CloseShiftRequest closeShiftRequest) {

        final Shift shift = shiftService.closeShift(client.getId(), closeShiftRequest.getCash(), closeShiftRequest.getCard());

        return toShiftResponse(shift);
    }

    @PostMapping("/confirmClose")
    public ShiftResponse closeShift(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                    @RequestBody String closingRemark) {

        Shift shift = shiftService.confirmCloseShift(client.getId(), closingRemark);

        return toShiftResponse(shift);
    }

    @PostMapping("/abortClose")
    public ShiftResponse abortCloseShift(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        Shift shift = shiftService.abortCloseShift(client.getId());

        return toShiftResponse(shift);
    }

    private ShiftResponse toShiftResponse(final Shift shift) {

        final ShiftResponse.OpenShiftDetailsResponse openShiftResponse = new ShiftResponse.OpenShiftDetailsResponse(
                shift.getStart().getTimestamp(),
                shift.getStart().getWho(),
                shift.getStart().getBalance());

        final ShiftResponse.CloseShiftDetailsResponse closeShiftResponse = new ShiftResponse.CloseShiftDetailsResponse(
                shift.getEnd().getTimestamp(),
                shift.getEnd().getWho(),
                shift.getEnd().getClosingShiftReport(),
                shift.getEnd().getClosingBalances(),
                shift.getEnd().getClosingRemark());

        return new ShiftResponse(shift.getId(),
                shift.getClientId(),
                shift.getShiftStatus(),
                openShiftResponse,
                closeShiftResponse);
    }
}