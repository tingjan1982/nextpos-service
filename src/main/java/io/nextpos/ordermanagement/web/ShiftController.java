package io.nextpos.ordermanagement.web;

import io.nextpos.client.data.Client;
import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.datetime.service.ZonedDateRangeBuilder;
import io.nextpos.ordermanagement.data.Shift;
import io.nextpos.ordermanagement.service.ShiftService;
import io.nextpos.ordermanagement.web.model.CloseShiftRequest;
import io.nextpos.ordermanagement.web.model.OpenShiftRequest;
import io.nextpos.ordermanagement.web.model.ShiftResponse;
import io.nextpos.ordermanagement.web.model.ShiftsResponse;
import io.nextpos.ordertransaction.data.ClosingShiftTransactionReport;
import io.nextpos.reporting.data.DateParameterType;
import io.nextpos.shared.web.ClientResolver;
import io.nextpos.workingarea.data.SinglePrintInstruction;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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

    @GetMapping
    public ShiftsResponse getShifts(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                    @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        final ZonedDateRange zonedDateRange = ZonedDateRangeBuilder.builder(client, DateParameterType.MONTH)
                .date(date).build();

        final List<ShiftResponse> shifts = shiftService.getShifts(client.getId(), zonedDateRange).stream()
                .map(this::toShiftResponse).collect(Collectors.toList());

        return new ShiftsResponse(zonedDateRange, shifts);
    }

    @GetMapping("/{shiftId}")
    public ShiftResponse getShift(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                  @PathVariable final String shiftId) {

        final Shift shift = shiftService.getShift(shiftId);

        return toShiftResponse(shift);
    }

    @GetMapping("/{shiftId}/report")
    public ClosingShiftTransactionReport getClosingShiftTransactionReport(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                                          @PathVariable final String shiftId) {
        return shiftService.getClosingShiftReport(client.getId(), shiftId);
    }

    @PostMapping("/{shiftId}/balance")
    public ShiftResponse balanceShift(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                      @PathVariable final String shiftId) {

        final Shift shift = shiftService.balanceClosingShift(shiftId);

        return toShiftResponse(shift);
    }

    @PostMapping("/{shiftId}/email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void emailShiftReport(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                 @PathVariable String shiftId) {

        String notificationEmail = client.getUsername();

        if (StringUtils.isNotBlank(client.getAttribute(Client.ClientAttributes.NOTIFICATION_EMAIL))) {
            notificationEmail = client.getAttribute(Client.ClientAttributes.NOTIFICATION_EMAIL);
        }

        shiftService.sendShiftReport(client, shiftId, notificationEmail);
    }

    @PostMapping("/{shiftId}/print")
    public SinglePrintInstruction printShiftReport(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                   @PathVariable String shiftId) {

        return shiftService.printShiftReport(client, shiftId);
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

        Map<String, Shift.ClosingBalanceDetails> closingBalances = Map.of("CASH", closeShiftRequest.getCash(), "CARD", closeShiftRequest.getCard());

        final Shift shift = shiftService.closeShift(client.getId(), closingBalances);

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
        return new ShiftResponse(shift);
    }
}