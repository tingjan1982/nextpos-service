package io.nextpos.settings.web.model;

import io.nextpos.settings.data.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class PaymentMethodsResponse {

    private final List<PaymentMethodResponse> results;

    public PaymentMethodsResponse(Set<PaymentMethod> paymentMethods) {
        this.results = paymentMethods.stream()
                .map(PaymentMethodResponse::new)
                .collect(Collectors.toList());
    }
}
