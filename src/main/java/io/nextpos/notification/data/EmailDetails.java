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
public class EmailDetails extends NotificationDetails {

    private String recipientEmail;

    private String subject;

    private String emailContent;

    public EmailDetails(final String clientId, final String recipientEmail, final String subject, final String emailContent) {
        super(clientId);
        this.recipientEmail = recipientEmail;
        this.subject = subject;
        this.emailContent = emailContent;
    }
}
