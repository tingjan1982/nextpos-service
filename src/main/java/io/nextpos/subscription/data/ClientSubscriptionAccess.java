package io.nextpos.subscription.data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ClientSubscriptionAccess {

    private List<String> restrictedFeatures;

    private int deviceLimit;

    private int currentUserLimit;

    public ClientSubscriptionAccess(ClientSubscription clientSubscription) {
        final SubscriptionPlan subscriptionPlan = clientSubscription.getSubscriptionPlanSnapshot();
        final SubscriptionPlan.SubscriptionPlanLimit accessControl = subscriptionPlan.getSubscriptionLimit();

        restrictedFeatures = accessControl.getRestrictedFeatures();
        deviceLimit = accessControl.getDeviceLimit();
        currentUserLimit = accessControl.getUserLimit();
    }
}
