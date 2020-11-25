package io.nextpos.announcement.service;

import io.nextpos.announcement.data.GlobalAnnouncement;

import java.util.List;

public interface GlobalAnnouncementService {

    GlobalAnnouncement createGlobalAnnouncement(String title, String content);

    GlobalAnnouncement saveGlobalAnnouncement(GlobalAnnouncement globalAnnouncement);

    GlobalAnnouncement getGlobalAnnouncement(String id);

    List<GlobalAnnouncement> getGlobalAnnouncements();

    void deleteGlobalAnnouncement(GlobalAnnouncement globalAnnouncement);
}
