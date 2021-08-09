package io.nextpos.subscription.web.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class RenewSubscriptionRequest {

    private LocalDate renewalDate;
}
