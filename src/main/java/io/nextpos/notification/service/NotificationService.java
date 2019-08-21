package io.nextpos.notification.service;

import io.nextpos.notification.data.NotificationDetails;

import java.util.concurrent.CompletableFuture;

public interface NotificationService {

    CompletableFuture<NotificationDetails> sendNotification(NotificationDetails notificationDetails);
}
