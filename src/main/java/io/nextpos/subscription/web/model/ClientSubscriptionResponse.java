package io.nextpos.subscription.web.model;

import io.nextpos.client.data.Client;
import io.nextpos.subscription.data.ClientSubscription;
import io.nextpos.subscription.data.SubscriptionPlan;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
public class ClientSubscriptionResponse {

    private String id;

    private String clientId;

    private String planName;

    private ClientSubscription.SubscriptionStatus status;

    private BigDecimal planPrice;

    private SubscriptionPlan.PlanPeriod planPeriod;

    private Date submittedDate;

    private Date planStartDate;

    private Date planEndDate;

    private String clientName;

    private String clientUsername;

    private String invoiceIdentifier;

    public ClientSubscriptionResponse(ClientSubscription clientSubscription) {
        id = clientSubscription.getId();
        clientId = clientSubscription.getClientId();
        planName = clientSubscription.getSubscriptionPlanSnapshot().getPlanName();
        status = clientSubscription.getStatus();
        planPrice = clientSubscription.getPlanPrice();
        planPeriod = clientSubscription.getPlanPeriod();
        submittedDate = clientSubscription.getSubmittedDate();
        planStartDate = clientSubscription.getPlanStartDate();
        planEndDate = clientSubscription.getPlanEndDate();
    }

    public static ClientSubscriptionResponse defaultResponse(Client client) {

        return new ClientSubscriptionResponse(null,
                client.getId(),
                "planName.free",
                ClientSubscription.SubscriptionStatus.ACTIVE,
                BigDecimal.ZERO,
                SubscriptionPlan.PlanPeriod.MONTHLY,
                null,
                null,
                null,
                client.getClientName(),
                client.getUsername(),
                null);
    }
}
