package io.nextpos.announcement.service;

import io.nextpos.announcement.data.GlobalAnnouncement;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ChainedTransaction
class GlobalAnnouncementServiceImplTest {

    private final GlobalAnnouncementService globalAnnouncementService;

    @Autowired
    GlobalAnnouncementServiceImplTest(GlobalAnnouncementService globalAnnouncementService) {
        this.globalAnnouncementService = globalAnnouncementService;
    }

    @Test
    void createGlobalAnnouncement() {

        final GlobalAnnouncement globalAnnouncement = globalAnnouncementService.createGlobalAnnouncement("New version released", "Check it out y'all!");

        assertThat(globalAnnouncement.getId()).isNotNull();

        globalAnnouncement.markAsRead("dummy-client", "dummy-device");
        globalAnnouncement.markAsRead("dummy-client", "dummy-device");
        globalAnnouncementService.saveGlobalAnnouncement(globalAnnouncement);

        assertThat(globalAnnouncementService.getGlobalAnnouncement(globalAnnouncement.getId())).satisfies(ga -> {
            assertThat(ga.getReadDevices()).isNotEmpty();
            assertThat(ga.getReadDevices().get("dummy-client")).hasSize(1);
        });

        assertThat(globalAnnouncementService.getGlobalAnnouncements()).isNotEmpty();

        globalAnnouncementService.deleteGlobalAnnouncement(globalAnnouncement);

        assertThat(globalAnnouncementService.getGlobalAnnouncements()).isEmpty();
    }

    @Test
    void checkTransactionBehavior() {
        assertThat(globalAnnouncementService.getGlobalAnnouncements()).isEmpty();
    }
}