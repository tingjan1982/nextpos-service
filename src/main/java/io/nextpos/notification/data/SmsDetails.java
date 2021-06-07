package io.nextpos.notification.data;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "notificationDetails")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class SmsDetails extends NotificationDetails {

    private String toNumber;

    private String message;

    public SmsDetails(final String clientId, final String toNumber, final String message) {
        super(clientId);
        this.toNumber = toNumber;
        this.message = message;
    }
}
