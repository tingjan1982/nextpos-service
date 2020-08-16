package io.nextpos.client.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.notification.data.NotificationDetails;

import java.util.concurrent.CompletableFuture;

public interface ClientActivationService {

    void initiateClientActivation(Client client);

    CompletableFuture<NotificationDetails> sendActivationNotification(Client client);

    ClientActivationServiceImpl.ActivationStatus activateClient(String encodedToken);

    CompletableFuture<NotificationDetails> sendResetPasscode(String clientEmail);

    boolean verifyResetPasscode(String clientEmail, String passcodeToVerify);

    ClientUser resetClientPassword(String clientEmail, String password);
}
