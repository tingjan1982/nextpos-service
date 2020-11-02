package io.nextpos.clienttracker.data;

import io.nextpos.client.data.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientUsageTrackRepository extends JpaRepository<ClientUsageTrack, String> {

    Optional<ClientUsageTrack> findByClientAndTrackingTypeAndValue(Client client, ClientUsageTrack.TrackingType trackingType, String value);

    long countAllByClientAndTrackingType(Client client, ClientUsageTrack.TrackingType trackingType);
}
