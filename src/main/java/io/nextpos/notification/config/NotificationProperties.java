package io.nextpos.notification.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "notifications.sms")
@Data
@NoArgsConstructor
public class NotificationProperties {

    private String accountSid;

    private String authToken;
}
