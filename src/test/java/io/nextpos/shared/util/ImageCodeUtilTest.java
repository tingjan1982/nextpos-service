package io.nextpos.shared.util;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

class ImageCodeUtilTest {

    @Test
    void generateQRCode() throws Exception {

        final BufferedImage bufferedImage = new ImageCodeUtil().generateQRCode("CR33734790109111295210000082f000008980000000083515813QErKH2hbLt4YoIFIDu75Iw==:**********:2:2:1:");
        System.out.println(bufferedImage);
        
        ImageIO.write(bufferedImage, "png", new File("/Users/jlin/Downloads/test.png"));
    }
}