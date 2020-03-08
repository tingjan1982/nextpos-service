package io.nextpos.ordertransaction.data;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.ordertransaction.util.InvoiceQRCodeEncryptor;
import io.nextpos.shared.model.MongoBaseObject;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.List;

/**
 * Electronic Invoice specification:
 * https://www.einvoice.nat.gov.tw/home/DownLoad?fileName=1532427864696_0.pdf
 * <p>
 * QR Codes:
 * https://invoice.ppmof.gov.tw/web_doc/onlineBook/docs_A4.pdf
 */
@Document
@Data
@EqualsAndHashCode(callSuper = true)
//@NoArgsConstructor
@RequiredArgsConstructor
public class ElectronicInvoice extends MongoBaseObject {

    @Id
    private String id;

    private final String orderId;

    /**
     * vendor's invoice ID
     */
    private final String invoiceId;

    private final String invoiceStatus;

    /**
     * official e-invoice number.
     */
    private final String invoiceNumber;

    /**
     * official invoice number period
     */
    private final InvoicePeriod invoicePeriod;

    private final String randomNumber;

    private final String invoiceCreatedDate;

    private final BigDecimal salesAmount;

    private final BigDecimal taxAmount;

    private final String sellerUbn;

    private String buyerUbn;

    private String barcodeContent;

    private String qrCode1Content;

    private String qrCode2Content;

    public String getFormattedInvoiceDate() {
        return String.format("%s年%s-%s月", invoicePeriod.getTaiwanYear(), invoicePeriod.getTwoDigitStartMonth(), invoicePeriod.getTwoDigitEndMonth());
    }

    public void generateBarcodeContent() {
        barcodeContent = invoicePeriod.formatInvoicePeriod() + invoiceNumber + randomNumber;
    }

    public void generateQrCode1Content(InvoiceQRCodeEncryptor invoiceQRCodeEncryptor, Order order) {

        final StringBuilder qrCodeContent = new StringBuilder();
        qrCodeContent.append(invoiceNumber);
        qrCodeContent.append(invoicePeriod.formatLongInvoicePeriod());
        qrCodeContent.append(randomNumber);
        qrCodeContent.append(toEightDigitHexadecimal(salesAmount));
        qrCodeContent.append(toEightDigitHexadecimal(salesAmount.add(taxAmount)));
        qrCodeContent.append(buyerUbn != null ? buyerUbn : "00000000");
        qrCodeContent.append(sellerUbn);
        qrCodeContent.append(encryptInvoiceNumber(invoiceQRCodeEncryptor)).append(":");
        qrCodeContent.append("**********").append(":");

        final List<OrderLineItem> orderLineItems = order.getOrderLineItems();

        qrCodeContent.append(orderLineItems.size()).append(":");
        qrCodeContent.append(orderLineItems.size()).append(":");
        qrCodeContent.append("1:"); // UTF-8 encoding

        final int splitIndex = orderLineItems.size() / 2;

        for (int i = 0; i < splitIndex; i++) {
            final OrderLineItem lineItem = orderLineItems.get(i);
            qrCodeContent.append(lineItem.getProductSnapshot().getName()).append(":")
                    .append(lineItem.getQuantity()).append(":")
                    .append(lineItem.getLineItemSubTotal());
        }

        this.qrCode1Content = qrCodeContent.toString();
    }

    private String toEightDigitHexadecimal(BigDecimal number) {

        final String hex = Long.toHexString(number.longValue());
        final StringBuilder leftPadding = new StringBuilder();

        for (int i = 0; i < 8 - hex.length(); i++) {
            leftPadding.append("0");
        }

        return leftPadding.append(hex).toString();
    }
    
    private String encryptInvoiceNumber(final InvoiceQRCodeEncryptor invoiceQRCodeEncryptor) {
        return invoiceQRCodeEncryptor.encode(invoiceNumber + randomNumber);
    }

    public void generateQrCode2Content(Order order) {

        final StringBuilder qrCodeContent = new StringBuilder();
        qrCodeContent.append("**");

        final List<OrderLineItem> orderLineItems = order.getOrderLineItems();
        final int splitIndex = orderLineItems.size() / 2;

        for (int i = splitIndex; i < orderLineItems.size(); i++) {
            final OrderLineItem lineItem = orderLineItems.get(i);
            qrCodeContent.append(lineItem.getProductSnapshot().getName()).append(":")
                    .append(lineItem.getQuantity()).append(":")
                    .append(lineItem.getLineItemSubTotal());
        }

        this.qrCode2Content = qrCodeContent.toString();
    }

    @Override
    public boolean isNew() {
        return id == null;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoicePeriod {

        private String year;

        private String startMonth;

        private String endMonth;

        public String formatInvoicePeriod() {
            return getTaiwanYear() + getTwoDigitEndMonth();
        }

        public String formatLongInvoicePeriod() {
            return getTaiwanYear() + getTwoDigitStartMonth() + getTwoDigitEndMonth();
        }

        private String getTaiwanYear() {
            return String.valueOf(Integer.parseInt(year) - 1911);
        }

        private String getTwoDigitStartMonth() {
            return twoDigitMonth(startMonth);
        }

        private String getTwoDigitEndMonth() {
            return twoDigitMonth(endMonth);
        }

        private String twoDigitMonth(String month) {
            return month.length() == 1 ? "0" + month : month;
        }
    }
}
