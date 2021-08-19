package io.nextpos.notification.service;

import io.nextpos.notification.data.PushNotification;

public interface PushNotificationService {

    PushNotification savePushNotification(String clientId, String token);

    PushNotification getPushNotificationByClientId(String clientId);
}
