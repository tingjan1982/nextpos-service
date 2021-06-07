package io.nextpos.notification.service;

import io.nextpos.notification.data.NotificationDetails;

import java.util.concurrent.CompletableFuture;

public interface NotificationService {

    void sendSimpleNotification(NotificationDetails notificationDetails);

    CompletableFuture<NotificationDetails> sendNotification(NotificationDetails notificationDetails);
}
