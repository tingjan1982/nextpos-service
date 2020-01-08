package io.nextpos.announcement.service;

import io.nextpos.announcement.data.Announcement;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AnnouncementServiceImplTest {

    @Autowired
    private AnnouncementService announcementService;


    @Test
    void saveAnnouncement() {

        final Announcement announcement = new Announcement("client", "icon", "Special", "lobster");

        final Announcement createdAnnouncement = announcementService.saveAnnouncement(announcement);

        assertThat(createdAnnouncement.getId()).isNotNull();

        final List<Announcement> announcements = announcementService.getAnnouncements("client");

        assertThat(announcements).hasSize(1);
    }
}