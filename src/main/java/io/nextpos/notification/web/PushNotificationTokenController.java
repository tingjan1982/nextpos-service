package io.nextpos.notification.web;

import io.nextpos.client.data.Client;
import io.nextpos.notification.data.PushNotification;
import io.nextpos.notification.service.PushNotificationService;
import io.nextpos.notification.web.model.PushNotificationRequest;
import io.nextpos.notification.web.model.PushNotificationResponse;
import io.nextpos.shared.web.ClientResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/pushNotifications")
public class PushNotificationTokenController {

    private final PushNotificationService pushNotificationService;

    @Autowired
    public PushNotificationTokenController(PushNotificationService pushNotificationService) {
        this.pushNotificationService = pushNotificationService;
    }

    @PostMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void savePushNotificationToken(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                          @Valid @RequestBody PushNotificationRequest request) {

        pushNotificationService.savePushNotification(client.getId(), request.getToken());
    }

    @GetMapping("/me")
    public PushNotificationResponse getPushNotificationToken(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        final PushNotification pushNotification = pushNotificationService.getPushNotificationByClientId(client.getId());

        return new PushNotificationResponse(pushNotification);
    }
}
