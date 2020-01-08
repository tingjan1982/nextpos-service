package io.nextpos.announcement.data;

import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface AnnouncementRepository extends PagingAndSortingRepository<Announcement, String> {

    List<Announcement> findAllByClientIdOrderByOrder(String clientId);
}
