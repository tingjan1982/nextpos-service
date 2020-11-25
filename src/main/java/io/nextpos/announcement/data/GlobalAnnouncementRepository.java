package io.nextpos.announcement.data;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface GlobalAnnouncementRepository extends MongoRepository<GlobalAnnouncement, String> {
}
