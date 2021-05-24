package io.nextpos.shared.util;

import java.util.Locale;
import java.util.ResourceBundle;

public class PaymentMethodLocalization {

    public static String getPaymentMethodTranslation(String paymentMethod) {

        try {
            ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.TRADITIONAL_CHINESE);
            return bundle.getString("paymentMethod." + paymentMethod);
        } catch (Exception e) {
            return null;
        }
    }
}
