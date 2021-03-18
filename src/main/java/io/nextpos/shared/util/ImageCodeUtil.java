package io.nextpos.shared.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code39Writer;
import com.google.zxing.qrcode.QRCodeWriter;
import io.nextpos.shared.exception.GeneralApplicationException;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
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
            final HashMap<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, "L");
            hints.put(EncodeHintType.QR_VERSION, "10");

            final BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 176, 176, hints);
            return MatrixToImageWriter.toBufferedImage(bitMatrix);

        } catch (Exception e) {
            throw new GeneralApplicationException("Error while generating code image: " + e.getMessage(), e);
        }
    }

    /**
     * Convert to 1 bit per pixel and ensure image color is not inverted (achieved on line 59 by swapping its order.
     * Code is copied from BufferedImage constructor.
     *
     * Reference:
     * https://stackoverflow.com/questions/14513542/how-to-convert-image-to-black-and-white-using-java
     */
    public String generateBase64ImageBinary(Supplier<BufferedImage> bufferedImageSupplier) {

        final BufferedImage image = bufferedImageSupplier.get();
        byte[] arr = {(byte) 0xff, (byte) 0};
        IndexColorModel colorModel = new IndexColorModel(1, 2, arr, arr, arr);
        BufferedImage blackWhite = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY, colorModel);
        Graphics2D g2d = blackWhite.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();

        final byte[] data = ((DataBufferByte) blackWhite.getData().getDataBuffer()).getData();

        return Base64.getEncoder().encodeToString(data);
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
