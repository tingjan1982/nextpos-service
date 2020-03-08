package io.nextpos.ordertransaction.util;

import io.nextpos.shared.exception.GeneralApplicationException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

/**
 * @author MrCuteJacky
 * @version 1.0
 */
public class InvoiceQRCodeEncryptor {

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

    private SecretKeySpec secretKeySpec;

    private Cipher cipher;

    private IvParameterSpec ivParameterSpec;

//    /**
//     * @param args
//     * @throws Exception
//     */
//    public static void main(String[] args) throws Exception {
//
//        // input PASSPHASE to get AESKEY with genKey.bat/ genKey.sh
//        String aesKey = "C03458ECE7485C884AC42D3ED8198A4C";// input your aeskey
//        String invoiceNumAndRandomCode = "AA123456781234";// input your invoiceNumber And RandomCode
//
//        InvoiceQRCodeEncryptor aes = new InvoiceQRCodeEncryptor(aesKey);
//
//        // DO AES ENCODE
//        String encoded = aes.encode(invoiceNumAndRandomCode);
//        System.out.println(invoiceNumAndRandomCode + " => " + encoded);
//
//        // DO AES DECODE
//        String decoded = aes.decode(encoded);
//        System.out.println(encoded + " => " + decoded);
//    }

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
