package io.nextpos.settings.web.model;

import io.nextpos.settings.data.PaymentMethod;
import lombok.Data;

@Data
public class PaymentMethodResponse {

    private final String id;

    private final String paymentKey;

    private final String displayName;

    public PaymentMethodResponse(PaymentMethod paymentMethod) {
        this.id = paymentMethod.getId();
        this.paymentKey = paymentMethod.getPaymentKey();
        this.displayName = paymentMethod.getDisplayName();
    }
}
