package io.nextpos.notification.web.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
public class PushNotificationRequest {

    @NotBlank
    private String token;
}
