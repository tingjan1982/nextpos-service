package io.nextpos.announcement.service;

import io.nextpos.announcement.data.GlobalAnnouncement;
import io.nextpos.announcement.data.GlobalAnnouncementRepository;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ChainedTransaction
public class GlobalAnnouncementServiceImpl implements GlobalAnnouncementService {

    private final GlobalAnnouncementRepository globalAnnouncementRepository;

    @Autowired
    public GlobalAnnouncementServiceImpl(GlobalAnnouncementRepository globalAnnouncementRepository) {
        this.globalAnnouncementRepository = globalAnnouncementRepository;
    }

    @Override
    public GlobalAnnouncement createGlobalAnnouncement(String title, String content) {
        
        return saveGlobalAnnouncement(new GlobalAnnouncement(title, content));
    }

    @Override
    public GlobalAnnouncement saveGlobalAnnouncement(GlobalAnnouncement globalAnnouncement) {
        return globalAnnouncementRepository.save(globalAnnouncement);
    }

    @Override
    public GlobalAnnouncement getGlobalAnnouncement(String id) {
        return globalAnnouncementRepository.findById(id).orElseThrow(() -> {
            throw new ObjectNotFoundException(id, GlobalAnnouncement.class);
        });
    }

    @Override
    public List<GlobalAnnouncement> getGlobalAnnouncements() {
        return globalAnnouncementRepository.findAll(Sort.by(Sort.Order.desc("createdDate")));
    }

    @Override
    public void deleteGlobalAnnouncement(GlobalAnnouncement globalAnnouncement) {
        globalAnnouncementRepository.delete(globalAnnouncement);
    }
}
