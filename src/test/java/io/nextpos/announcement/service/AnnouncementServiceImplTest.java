package io.nextpos.announcement.service;

import io.nextpos.announcement.data.Announcement;
import io.nextpos.shared.exception.ObjectNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

        announcementService.getAnnouncement(announcement.getId());

        final List<Announcement> announcements = announcementService.getAnnouncements("client");

        assertThat(announcements).hasSize(1);

        announcementService.deleteAnnouncement(announcement);

        assertThatThrownBy(() -> announcementService.getAnnouncement(announcement.getId())).isInstanceOf(ObjectNotFoundException.class);
    }
}