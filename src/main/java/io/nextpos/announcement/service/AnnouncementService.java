package io.nextpos.announcement.service;

import io.nextpos.announcement.data.Announcement;

import java.util.List;

public interface AnnouncementService {

    Announcement saveAnnouncement(Announcement announcement);

    Announcement getAnnouncement(String id);

    List<Announcement> getAnnouncements(String clientId);
}
