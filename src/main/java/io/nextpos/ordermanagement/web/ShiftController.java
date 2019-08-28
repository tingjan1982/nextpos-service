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

@RestController
@RequestMapping("/shifts")
public class ShiftController {

    private final ShiftService shiftService;

    @Autowired
    public ShiftController(final ShiftService shiftService) {
        this.shiftService = shiftService;
    }

    @PostMapping("/open")
    public ShiftResponse openShift(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                   @Valid @RequestBody ShiftRequest shiftRequest) {

        final Shift shift = shiftService.openShift(client.getId(), shiftRequest.getBalance());
        return toShiftResponse(shift);
    }

    @PostMapping("/close")
    public ShiftResponse closeShift(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                    @Valid @RequestBody ShiftRequest shiftRequest) {

        final Shift shift = shiftService.closeShift(client.getId(), shiftRequest.getBalance());
        return toShiftResponse(shift);
    }

    private ShiftResponse toShiftResponse(final Shift shift) {
        return new ShiftResponse(shift.getId(),
                shift.getShiftStatus(),
                shift.getStart().getTimestamp(),
                shift.getStart().getWho(),
                shift.getStart().getBalance());
    }
}