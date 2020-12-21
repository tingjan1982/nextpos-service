package io.nextpos.subscription.service;

import io.nextpos.shared.service.annotation.ChainedTransaction;
import io.nextpos.subscription.data.ClientSubscription;
import io.nextpos.subscription.data.ClientSubscriptionAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@ChainedTransaction
public class ClientSubscriptionAccessServiceImpl implements ClientSubscriptionAccessService {

    private final ClientSubscriptionService clientSubscriptionService;

    @Autowired
    public ClientSubscriptionAccessServiceImpl(ClientSubscriptionService clientSubscriptionService) {
        this.clientSubscriptionService = clientSubscriptionService;
    }

    @Override
    public ClientSubscriptionAccess getClientSubscriptionAccess(String clientId) {
        final ClientSubscription currentClientSubscription = clientSubscriptionService.getCurrentClientSubscription(clientId);

        if (currentClientSubscription != null && currentClientSubscription.isActiveSubscription()) {
            return new ClientSubscriptionAccess(currentClientSubscription);
        }

        return createDefaultClientSubscriptionAccess();
    }

    private ClientSubscriptionAccess createDefaultClientSubscriptionAccess() {

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
                "einvoice"
        ), 1, 1);
    }
    
}
