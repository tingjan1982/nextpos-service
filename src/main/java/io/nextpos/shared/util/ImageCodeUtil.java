package io.nextpos.shared.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code39Writer;
import com.google.zxing.qrcode.QRCodeWriter;
import io.nextpos.shared.exception.GeneralApplicationException;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.function.Supplier;

/**
 * https://www.baeldung.com/java-generating-barcodes-qr-codes
 */
@Component
public class ImageCodeUtil {

    public BufferedImage generateBarCode(String content) {

        try {
            final Code39Writer writer = new Code39Writer();
            final BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.CODE_39, 100, 30);
            return MatrixToImageWriter.toBufferedImage(bitMatrix);

        } catch (Exception e) {
            throw new GeneralApplicationException("Error while generating code image: " + e.getMessage(), e);
        }
    }

    public BufferedImage generateQRCode(String content) {

        try {
            final QRCodeWriter qrCodeWriter = new QRCodeWriter();
            final BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 41, 41);
            return MatrixToImageWriter.toBufferedImage(bitMatrix);

        } catch (Exception e) {
            throw new GeneralApplicationException("Error while generating code image: " + e.getMessage(), e);
        }
    }

    public String generateCodeAsBase64(Supplier<BufferedImage> bufferedImageProvider) {

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ImageIO.write(bufferedImageProvider.get(), "png", os);
            return Base64.getEncoder().encodeToString(os.toByteArray());

        } catch (Exception e) {
            throw new GeneralApplicationException(e.getMessage(), e);
        }
    }
}
