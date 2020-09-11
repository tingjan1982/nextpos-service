package io.nextpos.ordertransaction.data;

import io.nextpos.shared.exception.GeneralApplicationException;
import io.nextpos.shared.model.MongoBaseObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.chrono.ChronoZonedDateTime;
import java.time.chrono.MinguoChronology;
import java.time.chrono.MinguoDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Date;
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
@RequiredArgsConstructor
public class ElectronicInvoice extends MongoBaseObject {

    @Id
    private String id;

    private final String orderId;

    /**
     * The official electronic invoice number
     */
    private final String invoiceNumber;

    /**
     * The invoice number without the hyphen separator (i.e. -)
     */
    private final String internalInvoiceNumber;

    private final InvoiceStatus invoiceStatus;

    /**
     * official invoice number period
     */
    private final InvoicePeriod invoicePeriod;

    private final String randomNumber;

    private final Date invoiceCreatedDate;

    private final BigDecimal salesAmount;

    private final BigDecimal taxAmount;

    private final String sellerUbn;

    private String buyerUbn;

    private String barcodeContent;

    private String qrCode1Content;

    private String qrCode2Content;

    private List<InvoiceItem> invoiceItems;

    public ElectronicInvoice(String orderId, String invoiceNumber, InvoicePeriod invoicePeriod, BigDecimal salesAmount, BigDecimal taxAmount, String sellerUbn, List<InvoiceItem> invoiceItems) {
        this.orderId = orderId;
        this.invoiceNumber = invoiceNumber;
        this.internalInvoiceNumber = invoiceNumber.replace("-", "");
        this.invoiceStatus = InvoiceStatus.CREATED;
        this.invoicePeriod = invoicePeriod;
        this.randomNumber = RandomStringUtils.randomNumeric(4);
        this.invoiceCreatedDate = new Date();
        this.salesAmount = salesAmount;
        this.taxAmount = taxAmount;
        this.sellerUbn = sellerUbn;
        this.invoiceItems = invoiceItems;
    }

    public String getFormattedInvoiceDate() {
        return String.format("%s年%s-%s月", invoicePeriod.getYear(), invoicePeriod.getStartMonth(), invoicePeriod.getEndMonth());
    }

    public void generateCodeContent(String aesKey) {
        final InvoiceQRCodeEncryptor invoiceQRCodeEncryptor = new InvoiceQRCodeEncryptor(aesKey);

        this.generateBarcodeContent();
        this.generateQrCode1Content(invoiceQRCodeEncryptor);
        this.generateQrCode2Content();
    }

    private void generateBarcodeContent() {
        barcodeContent = invoicePeriod.formatInvoicePeriod() + internalInvoiceNumber + randomNumber;
    }

    private void generateQrCode1Content(InvoiceQRCodeEncryptor invoiceQRCodeEncryptor) {

        final StringBuilder qrCodeContent = new StringBuilder();
        qrCodeContent.append(internalInvoiceNumber);
        qrCodeContent.append(invoicePeriod.formatLongInvoicePeriod());
        qrCodeContent.append(randomNumber);
        qrCodeContent.append(toEightDigitHexadecimal(salesAmount.subtract(taxAmount).setScale(0, RoundingMode.UP)));
        qrCodeContent.append(toEightDigitHexadecimal(salesAmount));
        qrCodeContent.append(buyerUbn != null ? buyerUbn : "00000000");
        qrCodeContent.append(sellerUbn);
        qrCodeContent.append(encryptInvoiceNumber(invoiceQRCodeEncryptor)).append(":");
        qrCodeContent.append("**********").append(":");

        qrCodeContent.append(invoiceItems.size()).append(":");
        qrCodeContent.append(invoiceItems.size()).append(":");
        qrCodeContent.append("1:"); // UTF-8 encoding

        final int splitIndex = invoiceItems.size() / 2;

        for (int i = 0; i < splitIndex; i++) {
            final InvoiceItem invoiceItem = invoiceItems.get(i);
            qrCodeContent.append(invoiceItem.getProductName()).append(":")
                    .append(invoiceItem.getQuantity()).append(":")
                    .append(invoiceItem.getSubTotal());
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
        return invoiceQRCodeEncryptor.encode(internalInvoiceNumber + randomNumber);
    }

    private void generateQrCode2Content() {

        final StringBuilder qrCodeContent = new StringBuilder();
        qrCodeContent.append("**");

        final int splitIndex = invoiceItems.size() / 2;

        for (int i = splitIndex; i < invoiceItems.size(); i++) {
            final InvoiceItem lineItem = invoiceItems.get(i);
            qrCodeContent.append(lineItem.getProductName()).append(":")
                    .append(lineItem.getQuantity()).append(":")
                    .append(lineItem.getSubTotal());
        }

        this.qrCode2Content = qrCodeContent.toString();
    }

    @Override
    public boolean isNew() {
        return id == null;
    }

    @Data
    @AllArgsConstructor
    public static class InvoicePeriod {

        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MM");

        /**
         * Minguo year (e.g. 109 = 2020)
         */
        private String year;

        /**
         * Always start in odd month.
         *
         * e.g. 09
         */
        private String startMonth;

        /**
         * Always end in even month.
         *
         * e.g. 10
         */
        private String endMonth;

        public InvoicePeriod(ZoneId zoneId) {

            final ChronoZonedDateTime<MinguoDate> date = MinguoChronology.INSTANCE.zonedDateTime(Instant.now(), zoneId);
            this.year = String.valueOf(date.get(ChronoField.YEAR));
            final int month = date.get(ChronoField.MONTH_OF_YEAR);

            if (month % 2 == 0) {
                this.startMonth = date.minus(1, ChronoUnit.MONTHS).format(FORMATTER);
                this.endMonth = date.format(FORMATTER);
            } else {
                this.startMonth = date.format(FORMATTER);
                this.endMonth = date.plus(1, ChronoUnit.MONTHS).format(FORMATTER);
            }
        }

        public String formatInvoicePeriod() {
            return getYear() + getEndMonth();
        }

        public String formatLongInvoicePeriod() {
            return getYear() + getStartMonth() + getEndMonth();
        }
    }

    @Data
    @AllArgsConstructor
    public static class InvoiceItem {

        private String productName;

        private int quantity;

        private BigDecimal unitPrice;

        private BigDecimal subTotal;
    }

    public enum InvoiceStatus {

        CREATED,

        ISSUED,

        MIG_CREATED,

        UPLOADED,

        SUCCESS,

        ERROR,

        FAIL
    }

    /**
     * @author MrCuteJacky
     * @version 1.0
     */
    public static class InvoiceQRCodeEncryptor {

        /**
         * The SPEC type
         */
        private final static String TYPE_SPEC = "AES";

        /**
         * The INIT type.
         */
        private final static String TYPE_INIT = "AES/CBC/PKCS5Padding";

        /**
         * The SPEC key.
         */
        private final static String SPEC_KEY = "Dt8lyToo17X/XkXaQvihuA==";

        private final SecretKeySpec secretKeySpec;

        private final Cipher cipher;

        private final IvParameterSpec ivParameterSpec;

        public InvoiceQRCodeEncryptor(String aesKey) {

            try {
                ivParameterSpec = new IvParameterSpec(DatatypeConverter.parseBase64Binary(SPEC_KEY));
                secretKeySpec = new SecretKeySpec(DatatypeConverter.parseHexBinary(aesKey), TYPE_SPEC);
                cipher = Cipher.getInstance(TYPE_INIT);
            } catch (Exception e) {
                throw new GeneralApplicationException("Unable to create QR Code Encryptor: " + e.getMessage());
            }

        }

        public String encode(String input) {

            try {
                cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
                byte[] encoded = cipher.doFinal(input.getBytes());

                return DatatypeConverter.printBase64Binary(encoded);
            } catch (Exception e) {
                throw new GeneralApplicationException("Unable to encrypt string: " + e.getMessage());
            }
        }

        public String decode(String input) throws Exception {

            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            byte[] decoded = DatatypeConverter.parseBase64Binary(input);

            return new String(cipher.doFinal(decoded));
        }
    }
}
