package io.nextpos.client.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PushNotificationTokenResponse {

    private String clientId;

    private String token;
}
