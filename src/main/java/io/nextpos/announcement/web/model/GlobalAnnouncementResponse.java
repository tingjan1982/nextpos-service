package io.nextpos.announcement.web.model;

import io.nextpos.announcement.data.GlobalAnnouncement;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;
import java.util.Set;

@Data
@AllArgsConstructor
public class GlobalAnnouncementResponse {

    private String id;

    private String title;

    private String content;

    private Map<String, Set<String>> readDevices;

    public GlobalAnnouncementResponse(GlobalAnnouncement globalAnnouncement) {
        this.id = globalAnnouncement.getId();
        this.title = globalAnnouncement.getTitle();
        this.content = globalAnnouncement.getContent();
        this.readDevices = globalAnnouncement.getReadDevices();
    }
}
