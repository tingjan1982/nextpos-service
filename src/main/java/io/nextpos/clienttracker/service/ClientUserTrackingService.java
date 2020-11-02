package io.nextpos.clienttracker.service;

import io.nextpos.client.data.Client;
import io.nextpos.clienttracker.data.ClientUsageTrack;

public interface ClientUserTrackingService {

    ClientUsageTrack saveClientUsageTrack(Client client, ClientUsageTrack.TrackingType trackingType, String username);

    void deleteClientUsageTrack(Client client, ClientUsageTrack.TrackingType trackingType, String username);

    void trackClientUser(Client client, String username);

    void trackClientDevice(Client client, String ipAddress);
}
