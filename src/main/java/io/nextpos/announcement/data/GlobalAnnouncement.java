package io.nextpos.announcement.data;

import io.nextpos.shared.model.MongoBaseObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class GlobalAnnouncement extends MongoBaseObject {

    @Id
    private String id;

    private String title;

    private String content;

    /**
     * This specifies the country that the announcement will go to.
     */
    private String country;

    /**
     * Key is client id, value is a list of device ids.
     */
    private Map<String, Set<String>> readDevices = new HashMap<>();

    public GlobalAnnouncement(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public void clearReadDevices() {
        readDevices.clear();
    }

    public void markAsRead(String clientId, String deviceId) {
        final Set<String> deviceIds = readDevices.computeIfAbsent(clientId, c -> new HashSet<>());

        deviceIds.add(deviceId);
    }
}
