package io.nextpos.notification.web.model;

import io.nextpos.notification.data.PushNotification;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Set;

@Data
@AllArgsConstructor
public class PushNotificationResponse {

    private Set<String> tokens;

    public PushNotificationResponse(PushNotification pushNotification) {
        this.tokens = pushNotification.getTokens();
    }
}
