package io.nextpos.clienttracker.service;

import io.nextpos.client.data.Client;
import io.nextpos.clienttracker.data.ClientUsageTrack;
import io.nextpos.clienttracker.data.ClientUsageTrackRepository;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import io.nextpos.subscription.data.ClientSubscription;
import io.nextpos.subscription.service.ClientSubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@ChainedTransaction
public class ClientUsageTrackingServiceImpl implements ClientUserTrackingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientUsageTrackingServiceImpl.class);

    private final ClientUsageTrackRepository clientUsageTrackRepository;

    private final ClientSubscriptionService clientSubscriptionService;

    @Autowired
    public ClientUsageTrackingServiceImpl(ClientUsageTrackRepository clientUsageTrackRepository, ClientSubscriptionService clientSubscriptionService) {
        this.clientUsageTrackRepository = clientUsageTrackRepository;
        this.clientSubscriptionService = clientSubscriptionService;
    }

    @Override
    public ClientUsageTrack saveClientUsageTrack(Client client, ClientUsageTrack.TrackingType trackingType, String value) {

        final ClientUsageTrack clientUsageTrack = clientUsageTrackRepository.findByClientAndTrackingTypeAndValue(client, trackingType, value)
                .orElse(new ClientUsageTrack(client, trackingType, value));
        clientUsageTrack.setActiveStamp(new Date());

        return clientUsageTrackRepository.save(clientUsageTrack);
    }

    @Override
    public void deleteClientUsageTrack(Client client, ClientUsageTrack.TrackingType trackingType, String username) {
        clientUsageTrackRepository.findByClientAndTrackingTypeAndValue(client, trackingType, username).ifPresent(clientUsageTrackRepository::delete);
    }

    @Override
    public void trackClientUser(Client client, String username) {

        this.saveClientUsageTrack(client, ClientUsageTrack.TrackingType.USER, username);

        final long usageCount = clientUsageTrackRepository.countAllByClientAndTrackingType(client, ClientUsageTrack.TrackingType.USER);
        final int userLimit = getUserLimit(client);

        handleLimitReached(usageCount, userLimit, "message.userLimitReached");
    }

    private int getUserLimit(Client client) {
        final ClientSubscription clientSubscription = clientSubscriptionService.getCurrentClientSubscription(client.getId());

        return clientSubscription != null ? clientSubscription.getSubscriptionPlanSnapshot().getUserLimit() : 2;
    }

    @Override
    public void trackClientDevice(Client client, String ipAddress) {
        this.saveClientUsageTrack(client, ClientUsageTrack.TrackingType.DEVICE, ipAddress);

        final long usageCount = clientUsageTrackRepository.countAllByClientAndTrackingType(client, ClientUsageTrack.TrackingType.DEVICE);
        final int deviceLimit = getDeviceLimit(client);

        handleLimitReached(usageCount, deviceLimit, "message.deviceLimitReached");
    }

    private int getDeviceLimit(Client client) {
        final ClientSubscription clientSubscription = clientSubscriptionService.getCurrentClientSubscription(client.getId());

        return clientSubscription != null ? clientSubscription.getSubscriptionPlanSnapshot().getDeviceLimit() : 1;
    }

    private void handleLimitReached(long usageCount, int limit, String messageKey) {

        if (usageCount > limit) {
            LOGGER.warn("Maximum number of limit has reached. Usage={}, limit={}", usageCount, limit);
            //throw new BusinessLogicException(messageKey, "Maximum number of limit has reached: " + limit);
        }
    }
}
