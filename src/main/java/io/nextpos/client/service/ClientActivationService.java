package io.nextpos.client.service;

import io.nextpos.client.data.Client;
import io.nextpos.notification.data.NotificationDetails;

import java.util.concurrent.CompletableFuture;

public interface ClientActivationService {

    CompletableFuture<NotificationDetails> sendActivationNotification(Client client);

    ClientActivationServiceImpl.ActivationStatus activateClient(String encodedToken);
}
