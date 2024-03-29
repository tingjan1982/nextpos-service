package io.nextpos.notification.data;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.bson.types.Binary;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;

@Document(collection = "notificationDetails")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class DynamicEmailDetails extends NotificationDetails {

    private String recipientEmail;

    private String templateId;

    private Map<String, Object> templateData = new HashMap<>();

    private Binary attachment;

    private String contentType;

    private String filename;

    public DynamicEmailDetails(String clientId, String recipientEmail, String templateId) {
        super(clientId);

        this.recipientEmail = recipientEmail;
        this.templateId = templateId;
    }

    public void addTemplateData(String key, Object value) {
        templateData.put(key, value);
    }
}
