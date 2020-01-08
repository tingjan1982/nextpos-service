package io.nextpos.announcement.service;

import io.nextpos.announcement.data.Announcement;

import java.util.List;

public interface AnnouncementService {

    Announcement saveAnnouncement(Announcement announcement);

    List<Announcement> getAnnouncements(String clientId);
}
