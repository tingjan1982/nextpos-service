package io.nextpos.notification.data;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PushNotificationRepository extends MongoRepository<PushNotification, String> {

    Optional<PushNotification> findByClientId(String clientId);
}
