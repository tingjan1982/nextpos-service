package io.nextpos.invoicenumber.web;

import io.nextpos.client.data.Client;
import io.nextpos.invoicenumber.web.model.ElectronicInvoiceEligibility;
import io.nextpos.ordertransaction.service.ElectronicInvoiceService;
import io.nextpos.shared.web.ClientResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/einvoices")
public class ElectronicInvoiceController {

    private final ElectronicInvoiceService electronicInvoiceService;

    @Autowired
    public ElectronicInvoiceController(ElectronicInvoiceService electronicInvoiceService) {
        this.electronicInvoiceService = electronicInvoiceService;
    }

    @GetMapping("/checkEligibility")
    public ElectronicInvoiceEligibility generateAESKey(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        return new ElectronicInvoiceEligibility(electronicInvoiceService.checkElectronicInvoiceEligibility(client));
    }
}
