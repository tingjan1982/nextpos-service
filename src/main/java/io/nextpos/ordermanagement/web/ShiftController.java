package io.nextpos.ordermanagement.web;

import io.nextpos.client.data.Client;
import io.nextpos.ordermanagement.data.Shift;
import io.nextpos.ordermanagement.service.ShiftService;
import io.nextpos.ordermanagement.web.model.ShiftRequest;
import io.nextpos.ordermanagement.web.model.ShiftResponse;
import io.nextpos.shared.web.ClientResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
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

    @PostMapping("/open")
    public ShiftResponse openShift(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                   @Valid @RequestBody ShiftRequest shiftRequest) {

        final Shift shift = shiftService.openShift(client.getId(), shiftRequest.getBalance());
        return toShiftResponse(shift);
    }

    @PostMapping("/interim")
    public ShiftResponse interimBalance(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                        @Valid @RequestBody ShiftRequest shiftRequest) {

        final Shift shift = shiftService.createInterimBalance(client.getId(), shiftRequest.getBalance());
        return toShiftResponse(shift);
    }

    @PostMapping("/close")
    public ShiftResponse closeShift(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                    @Valid @RequestBody ShiftRequest shiftRequest) {

        final Shift shift = shiftService.closeShift(client.getId(), shiftRequest.getBalance());
        return toShiftResponse(shift);
    }

    private ShiftResponse toShiftResponse(final Shift shift) {
        BigDecimal difference = null;

        if (shift.getShiftStatus() == Shift.ShiftStatus.UNBALANCED) {
            difference = shift.getEnd().getBalance().subtract(shift.getStart().getBalance());
        }

        final List<ShiftResponse.ShiftDetailsResponse> interimBalances = shift.getInterimBalances().stream()
                .map(sd -> new ShiftResponse.ShiftDetailsResponse(sd.getTimestamp(), sd.getWho(), sd.getBalance()))
                .collect(Collectors.toList());

        return new ShiftResponse(shift.getId(),
                shift.getClientId(),
                shift.getShiftStatus(),
                new ShiftResponse.ShiftDetailsResponse(shift.getStart().getTimestamp(), shift.getStart().getWho(), shift.getStart().getBalance()),
                interimBalances,
                new ShiftResponse.ShiftDetailsResponse(shift.getEnd().getTimestamp(), shift.getEnd().getWho(), shift.getEnd().getBalance()),
                difference);
    }
}