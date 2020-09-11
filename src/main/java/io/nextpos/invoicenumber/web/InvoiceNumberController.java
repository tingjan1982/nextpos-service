package io.nextpos.invoicenumber.web;

import io.nextpos.einvoice.common.invoicenumber.InvoiceNumberRange;
import io.nextpos.einvoice.common.invoicenumber.InvoiceNumberRangeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

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
    public InvoiceNumberRange addNumberRange(@PathVariable String ubn,
                                                   @PathVariable String rangeIdentifier,
                                                   @Valid @RequestBody InvoiceNumberRequest.NumberRangeRequest request) {

        final InvoiceNumberRange invoiceNumberRange = invoiceNumberRangeService.getInvoiceNumberRangeByRangeIdentifier(ubn, rangeIdentifier);
        invoiceNumberRange.addNumberRange(request.getPrefix(), request.getRangeFrom(), request.getRangeTo());

        return invoiceNumberRangeService.saveInvoiceNumberRange(invoiceNumberRange);
    }

    @GetMapping("/{ubn}")
    public List<InvoiceNumberRange> getInvoiceNumberRanges(@PathVariable String ubn) {
        return invoiceNumberRangeService.getInvoiceNumberRanges(ubn);
    }

    @GetMapping("/{ubn}/ranges/{rangeIdentifier}")
    public InvoiceNumberRange getInvoiceNumberRangeByRangeIdentifier(@PathVariable String ubn,
                                                                     @PathVariable String rangeIdentifier) {

        return invoiceNumberRangeService.getInvoiceNumberRangeByRangeIdentifier(ubn, rangeIdentifier);
    }
}
