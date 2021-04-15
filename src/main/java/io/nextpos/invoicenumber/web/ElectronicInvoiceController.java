package io.nextpos.invoicenumber.web;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientSetting;
import io.nextpos.client.service.ClientSettingsService;
import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.datetime.service.ZonedDateRangeBuilder;
import io.nextpos.einvoice.common.invoice.ElectronicInvoice;
import io.nextpos.einvoice.common.invoicenumber.InvoiceNumberRange;
import io.nextpos.einvoice.common.invoicenumber.InvoiceNumberRangeService;
import io.nextpos.invoicenumber.web.model.ElectronicInvoiceEligibility;
import io.nextpos.invoicenumber.web.model.ElectronicInvoiceResponse;
import io.nextpos.invoicenumber.web.model.ElectronicInvoicesResponse;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.web.model.OrdersByRangeResponse;
import io.nextpos.ordertransaction.service.ElectronicInvoiceService;
import io.nextpos.ordertransaction.service.OrderTransactionService;
import io.nextpos.reporting.data.DateParameterType;
import io.nextpos.shared.exception.GeneralApplicationException;
import io.nextpos.shared.util.ImageCodeUtil;
import io.nextpos.shared.web.ClientResolver;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/einvoices")
public class ElectronicInvoiceController {

    private final ElectronicInvoiceService electronicInvoiceService;

    private final InvoiceNumberRangeService invoiceNumberRangeService;

    private final OrderTransactionService orderTransactionService;

    private final ClientSettingsService clientSettingsService;

    private final ImageCodeUtil imageCodeUtil;

    @Autowired
    public ElectronicInvoiceController(ElectronicInvoiceService electronicInvoiceService, InvoiceNumberRangeService invoiceNumberRangeService, OrderTransactionService orderTransactionService, ClientSettingsService clientSettingsService, ImageCodeUtil imageCodeUtil) {
        this.electronicInvoiceService = electronicInvoiceService;
        this.invoiceNumberRangeService = invoiceNumberRangeService;
        this.orderTransactionService = orderTransactionService;
        this.clientSettingsService = clientSettingsService;
        this.imageCodeUtil = imageCodeUtil;
    }

    @GetMapping("/checkEligibility")
    public ElectronicInvoiceEligibility checkEligibility(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        boolean electronicInvoiceEnabled = clientSettingsService.getClientSettingBooleanValue(client, ClientSetting.SettingName.ELECTRONIC_INVOICE);

        return new ElectronicInvoiceEligibility(electronicInvoiceService.checkElectronicInvoiceEligibility(client), electronicInvoiceEnabled);
    }

    @GetMapping
    public ElectronicInvoicesResponse getElectronicInvoiceByStatus(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                                   @RequestParam("invoiceStatus") ElectronicInvoice.InvoiceStatus invoiceStatus) {

        final List<ElectronicInvoiceResponse> results = electronicInvoiceService.getElectronicInvoicesByInvoiceStatus(client, invoiceStatus).stream()
                .map(ElectronicInvoiceResponse::new)
                .collect(Collectors.toList());

        return new ElectronicInvoicesResponse(results);
    }

    @GetMapping("/cancellable")
    public OrdersByRangeResponse getCancellableInvoices(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        final String ubn = client.getAttribute(Client.ClientAttributes.UBN);
        final InvoiceNumberRange currentRange = invoiceNumberRangeService.getCurrentInvoiceNumberRange(ubn);
        final Pair<LocalDateTime, LocalDateTime> dateRange = currentRange.getLocalDateTimeRange();
        final ZonedDateRange zonedDateRange = ZonedDateRangeBuilder.builder(client, DateParameterType.RANGE)
                .dateRange(dateRange.getLeft(), dateRange.getRight())
                .build();

        final List<Order> cancellableOrders = orderTransactionService.getCancellableOrders(client, zonedDateRange);

        return new OrdersByRangeResponse(zonedDateRange, cancellableOrders);
    }

    @PostMapping("/toggle")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void toggleElectronicInvoice(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                        @RequestParam("enabled") Boolean enabled) {

        final ClientSetting clientSetting = clientSettingsService.getClientSettingByName(client, ClientSetting.SettingName.ELECTRONIC_INVOICE).orElseGet(() -> {
            return new ClientSetting(client, ClientSetting.SettingName.ELECTRONIC_INVOICE, "false", ClientSetting.ValueType.BOOLEAN, true);
        });

        clientSetting.setStoredValue(enabled.toString());
        clientSettingsService.saveClientSettings(clientSetting);
    }

    @PostMapping("/{id}/issueInvoiceNumber")
    public ElectronicInvoiceResponse getElectronicInvoiceByStatus(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                                  @PathVariable String id) {

        final ElectronicInvoice electronicInvoice = electronicInvoiceService.getElectronicInvoice(id);
        electronicInvoiceService.issueNewInvoiceNumber(electronicInvoice);

        return new ElectronicInvoiceResponse(electronicInvoice);
    }

    @GetMapping(value = "/{invoiceNumber}/code", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<Object> renderCode(@PathVariable String invoiceNumber,
                                             @RequestParam("code") String code,
                                             @RequestParam(name = "format", defaultValue = "stream") String format) {

        final ElectronicInvoice electronicInvoice = electronicInvoiceService.getElectronicInvoiceByInvoiceNumber(invoiceNumber);
        BufferedImage bufferedImage;

        switch (code) {
            case "barcode":
                bufferedImage = imageCodeUtil.generateBarCode(electronicInvoice.getBarcodeContent());
                break;
            case "qrcode1":
                bufferedImage = imageCodeUtil.generateQRCode(electronicInvoice.getQrCode1Content());
                break;
            case "qrcode2":
                bufferedImage = imageCodeUtil.generateQRCode(electronicInvoice.getQrCode2Content());
                break;
            default:
                throw new GeneralApplicationException("Invalid imageCode is provided: " + code);
        }

        if (format.equals("stream")) {
            return ResponseEntity.ok(bufferedImage);
        } else {
            final String base64EncodedImage = imageCodeUtil.generateCodeAsBase64(() -> bufferedImage);
            return ResponseEntity.ok(base64EncodedImage);
        }
    }
}
