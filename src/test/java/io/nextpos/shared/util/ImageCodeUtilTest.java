package io.nextpos.shared.util;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

class ImageCodeUtilTest {

    @Test
    void generateQRCode() throws Exception {

        final BufferedImage bufferedImage = new ImageCodeUtil().generateQRCode("1D286B8A003150304147313030303030303631303931323032343731323030303030326336303030303032653932373235323231303833353135383133645667487473384972557777643476376967513164773D3D3A2A2A2A2A2A2A2A2A2A2A3A31323A31323A323A2020202020202020202020202020202020202020202020202020202020202020202020202020");
        System.out.println(bufferedImage);
        
        ImageIO.write(bufferedImage, "png", new File("/Users/jlin/Downloads/test.png"));
    }
}