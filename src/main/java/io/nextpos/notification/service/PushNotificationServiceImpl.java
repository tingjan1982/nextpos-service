package io.nextpos.notification.service;

import io.nextpos.notification.data.PushNotification;
import io.nextpos.notification.data.PushNotificationRepository;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@ChainedTransaction
public class PushNotificationServiceImpl implements PushNotificationService {

    private final PushNotificationRepository pushNotificationRepository;

    @Autowired
    public PushNotificationServiceImpl(PushNotificationRepository pushNotificationRepository) {
        this.pushNotificationRepository = pushNotificationRepository;
    }

    @Override
    public PushNotification savePushNotification(String clientId, String token) {

        final PushNotification pushNotification = pushNotificationRepository.findByClientId(clientId).orElseGet(() -> new PushNotification(clientId));

        pushNotification.addToken(token);

        return pushNotificationRepository.save(pushNotification);
    }

    @Override
    public PushNotification getPushNotificationByClientId(String clientId) {

        return pushNotificationRepository.findByClientId(clientId).orElseThrow(() -> {
            throw new ObjectNotFoundException(clientId, PushNotification.class);
        });
    }
}
