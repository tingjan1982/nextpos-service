package io.nextpos.subscription.web.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
public class ActivateSubscriptionRequest {

    @NotBlank
    private String invoiceIdentifier;
}
