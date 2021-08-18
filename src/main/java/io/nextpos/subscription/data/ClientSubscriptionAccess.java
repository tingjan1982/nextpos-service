package io.nextpos.subscription.data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Arrays;
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

    public ClientSubscriptionAccess(SubscriptionPlan.SubscriptionPlanLimit limit) {

        this.restrictedFeatures = limit.getRestrictedFeatures();
        this.deviceLimit = limit.getDeviceLimit();
        this.currentUserLimit = limit.getUserLimit();
    }

    public static ClientSubscriptionAccess defaultClientSubscriptionAccess() {

        return new ClientSubscriptionAccess(Arrays.asList(
                "timeCard",
                "orderDisplay",
                "salesReport",
                "customerStats",
                "timeCardReport",
                "membership",
                "calendar",
                "staff",
                "roster",
                "reservation",
                "einvoice"
        ), 1, 1);
    }
}
