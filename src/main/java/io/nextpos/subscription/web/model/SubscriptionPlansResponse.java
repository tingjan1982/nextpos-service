package io.nextpos.subscription.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SubscriptionPlansResponse {

    private List<SubscriptionPlanResponse> results;
}
