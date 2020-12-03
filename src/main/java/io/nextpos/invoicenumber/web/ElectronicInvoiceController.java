package io.nextpos.invoicenumber.web;

import io.nextpos.client.data.Client;
import io.nextpos.einvoice.common.invoice.ElectronicInvoice;
import io.nextpos.invoicenumber.web.model.ElectronicInvoiceEligibility;
import io.nextpos.ordertransaction.service.ElectronicInvoiceService;
import io.nextpos.shared.exception.GeneralApplicationException;
import io.nextpos.shared.util.ImageCodeUtil;
import io.nextpos.shared.web.ClientResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.awt.image.BufferedImage;

@RestController
@RequestMapping("/einvoices")
public class ElectronicInvoiceController {

    private final ElectronicInvoiceService electronicInvoiceService;

    private final ImageCodeUtil imageCodeUtil;

    @Autowired
    public ElectronicInvoiceController(ElectronicInvoiceService electronicInvoiceService, ImageCodeUtil imageCodeUtil) {
        this.electronicInvoiceService = electronicInvoiceService;
        this.imageCodeUtil = imageCodeUtil;
    }

    @GetMapping("/checkEligibility")
    public ElectronicInvoiceEligibility generateAESKey(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        return new ElectronicInvoiceEligibility(electronicInvoiceService.checkElectronicInvoiceEligibility(client));
    }

    @GetMapping("/{invoiceNumber}/imageCode")
    public ResponseEntity<Object> renderCode(@PathVariable String invoiceNumber,
                                             @RequestParam("imageCode") String code,
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
