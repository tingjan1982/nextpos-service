package io.nextpos.announcement.service;

import io.nextpos.announcement.data.Announcement;
import io.nextpos.announcement.data.AnnouncementRepository;
import io.nextpos.shared.exception.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;

@Service
@Transactional
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementRepository announcementRepository;

    @Autowired
    public AnnouncementServiceImpl(final AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }

    @Override
    public Announcement saveAnnouncement(final Announcement announcement) {
        return announcementRepository.save(announcement);
    }

    @Override
    public Announcement getAnnouncement(final String id) {
        return announcementRepository.findById(id).orElseThrow(() -> {
            throw new ObjectNotFoundException(id, Announcement.class);
        });
    }

    @Override
    public List<Announcement> getAnnouncements(String clientId) {
        return announcementRepository.findAllByClientIdOrderByOrder(clientId);
    }

    @Override
    public void deleteAnnouncement(final Announcement announcement) {
        announcementRepository.delete(announcement);
    }
}
