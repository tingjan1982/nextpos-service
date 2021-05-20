package io.nextpos.subscription.service;

import io.nextpos.shared.service.annotation.ChainedTransaction;
import io.nextpos.subscription.data.ClientSubscription;
import io.nextpos.subscription.data.ClientSubscriptionAccess;
import io.nextpos.subscription.data.ClientSubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@ChainedTransaction
public class ClientSubscriptionAccessServiceImpl implements ClientSubscriptionAccessService {

    /**
     * Use this instead of ClientSubscriptionService to circumvent a circular dependency issue
     * (WebConfig -> ClientUsageTracker .. ClientSubscriptionService -> ClientSettingService -> EnableMvcAutoConfiguration)
     */
    private final ClientSubscriptionRepository clientSubscriptionRepository;

    @Autowired
    public ClientSubscriptionAccessServiceImpl(ClientSubscriptionRepository clientSubscriptionRepository) {
        this.clientSubscriptionRepository = clientSubscriptionRepository;
    }

    @Override
    public ClientSubscriptionAccess getClientSubscriptionAccess(String clientId) {
        final ClientSubscription currentClientSubscription = clientSubscriptionRepository.findByClientIdAndCurrentIsTrue(clientId);

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
                "reservation",
                "einvoice"
        ), 1, 1);
    }
    
}
