package io.nextpos.invoicenumber.web;

import io.nextpos.einvoice.common.invoicenumber.InvoiceNumberRange;
import io.nextpos.einvoice.common.invoicenumber.InvoiceNumberRangeService;
import io.nextpos.invoicenumber.web.model.InvoiceNumberRangeResponse;
import io.nextpos.invoicenumber.web.model.InvoiceNumberRangesResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/invoiceNumbers")
public class InvoiceNumberController {

    private final InvoiceNumberRangeService invoiceNumberRangeService;

    @Autowired
    public InvoiceNumberController(InvoiceNumberRangeService invoiceNumberRangeService) {
        this.invoiceNumberRangeService = invoiceNumberRangeService;
    }

    @PostMapping
    public InvoiceNumberRange createInvoiceNumberRange(@Valid @RequestBody InvoiceNumberRequest request) {

        InvoiceNumberRange invoiceNumberRange = fromRequest(request);
        return invoiceNumberRangeService.saveInvoiceNumberRange(invoiceNumberRange);
    }

    private InvoiceNumberRange fromRequest(InvoiceNumberRequest request) {
        final InvoiceNumberRequest.NumberRangeRequest numberRange = request.getNumberRange();

        return new InvoiceNumberRange(request.getUbn(), request.getRangeIdentifier(), numberRange.getPrefix(), numberRange.getRangeFrom(), numberRange.getRangeTo());
    }

    @PostMapping("/{ubn}/ranges/{rangeIdentifier}")
    public InvoiceNumberRangeResponse addNumberRange(@PathVariable String ubn,
                                                     @PathVariable String rangeIdentifier,
                                                     @Valid @RequestBody InvoiceNumberRequest.NumberRangeRequest request) {

        final InvoiceNumberRange invoiceNumberRange = invoiceNumberRangeService.getInvoiceNumberRangeByRangeIdentifier(ubn, rangeIdentifier);
        invoiceNumberRange.addNumberRange(request.getPrefix(), request.getRangeFrom(), request.getRangeTo());

        final InvoiceNumberRange savedInvoiceNumberRange = invoiceNumberRangeService.saveInvoiceNumberRange(invoiceNumberRange);

        return toInvoiceNumberRangeResponse(savedInvoiceNumberRange);
    }

    @GetMapping("/{ubn}")
    public InvoiceNumberRangeResponse getCurrentInvoiceNumberRange(@PathVariable String ubn) {

        final InvoiceNumberRange invoiceNumberRange = invoiceNumberRangeService.getCurrentInvoiceNumberRange(ubn);
        final InvoiceNumberRange.NumberRange dispensableNumberRange = invoiceNumberRange.findAvailableNumberRange();

        return toInvoiceNumberRangeResponse(invoiceNumberRange, dispensableNumberRange);
    }

    @GetMapping("/{ubn}/all")
    public InvoiceNumberRangesResponse getInvoiceNumberRanges(@PathVariable String ubn) {

        final List<InvoiceNumberRangeResponse> results = invoiceNumberRangeService.getInvoiceNumberRanges(ubn).stream()
                .map(this::toInvoiceNumberRangeResponse).collect(Collectors.toList());

        return new InvoiceNumberRangesResponse(results);
    }

    @GetMapping("/{ubn}/ranges/{rangeIdentifier}")
    public InvoiceNumberRangeResponse getInvoiceNumberRangeByRangeIdentifier(@PathVariable String ubn,
                                                                             @PathVariable String rangeIdentifier) {

        final InvoiceNumberRange invoiceNumberRange = invoiceNumberRangeService.getInvoiceNumberRangeByRangeIdentifier(ubn, rangeIdentifier);

        return toInvoiceNumberRangeResponse(invoiceNumberRange);
    }

    private InvoiceNumberRangeResponse toInvoiceNumberRangeResponse(InvoiceNumberRange invoiceNumberRange) {

        final List<InvoiceNumberRangeResponse.NumberRangeResponse> numberRanges = invoiceNumberRange.getNumberRanges().stream()
                .map(this::toNumberRangeResponse).collect(Collectors.toList());

        return new InvoiceNumberRangeResponse(invoiceNumberRange.getUbn(),
                invoiceNumberRange.getRangeIdentifier(),
                numberRanges);
    }

    private InvoiceNumberRangeResponse toInvoiceNumberRangeResponse(InvoiceNumberRange invoiceNumberRange, InvoiceNumberRange.NumberRange dispensableNumberRange) {

        return new InvoiceNumberRangeResponse(invoiceNumberRange.getUbn(),
                invoiceNumberRange.getRangeIdentifier(),
                List.of(toNumberRangeResponse(dispensableNumberRange)));
    }

    private InvoiceNumberRangeResponse.NumberRangeResponse toNumberRangeResponse(InvoiceNumberRange.NumberRange numberRange) {

        return new InvoiceNumberRangeResponse.NumberRangeResponse(numberRange.getPrefix(),
                numberRange.getRangeFrom(),
                numberRange.getRangeTo(),
                numberRange.isStarted(),
                numberRange.isFinished(),
                numberRange.getRemainingNumberInRange());
    }

    @DeleteMapping("/{ubn}/ranges/{rangeIdentifier}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInvoiceNumber(@PathVariable String ubn,
                                    @PathVariable String rangeIdentifier) {

        invoiceNumberRangeService.deleteInvoiceNumberRange(ubn, rangeIdentifier);
    }

    @PostMapping("/{ubn}/ranges/{rangeIdentifier}/numberRanges/{rangeFrom}/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disableInvoiceNumberRange(@PathVariable String ubn,
                                          @PathVariable String rangeIdentifier,
                                          @PathVariable String rangeFrom) {

        invoiceNumberRangeService.disableOneInvoiceNumberRange(ubn, rangeIdentifier, rangeFrom);
    }

    @DeleteMapping("/{ubn}/ranges/{rangeIdentifier}/numberRanges/{rangeFrom}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInvoiceNumberRange(@PathVariable String ubn,
                                         @PathVariable String rangeIdentifier,
                                         @PathVariable String rangeFrom) {

        invoiceNumberRangeService.deleteOneInvoiceNumberRange(ubn, rangeIdentifier, rangeFrom);
    }
}
