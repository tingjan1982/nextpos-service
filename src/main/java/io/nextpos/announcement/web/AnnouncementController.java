package io.nextpos.announcement.web;

import io.nextpos.announcement.data.Announcement;
import io.nextpos.announcement.service.AnnouncementService;
import io.nextpos.announcement.web.model.AnnouncementRequest;
import io.nextpos.announcement.web.model.AnnouncementResponse;
import io.nextpos.announcement.web.model.AnnouncementsResponse;
import io.nextpos.client.data.Client;
import io.nextpos.shared.web.ClientResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @Autowired
    public AnnouncementController(final AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @PostMapping
    public AnnouncementResponse createAnnouncement(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                   @Valid @RequestBody AnnouncementRequest announcementRequest) {

        Announcement announcement = fromRequest(client, announcementRequest);
        final Announcement savedAnnouncement = announcementService.saveAnnouncement(announcement);

        return toResponse(savedAnnouncement);
    }

    @GetMapping
    public AnnouncementsResponse getAnnouncements(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        final List<Announcement> announcements = announcementService.getAnnouncements(client.getId());

        final List<AnnouncementResponse> results = announcements.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new AnnouncementsResponse(results);
    }

    private Announcement fromRequest(final Client client, final AnnouncementRequest announcementRequest) {

        final Announcement announcement = new Announcement(client.getId(),
                announcementRequest.getTitleIcon(),
                announcementRequest.getTitle(),
                announcementRequest.getMarkdownContent());

        announcement.setOrder(announcementRequest.getOrder());

        return announcement;
    }

    private AnnouncementResponse toResponse(final Announcement savedAnnouncement) {

        return new AnnouncementResponse(savedAnnouncement.getId(),
                savedAnnouncement.getTitleIcon(),
                savedAnnouncement.getTitleText(),
                savedAnnouncement.getMarkdownContent(),
                savedAnnouncement.getOrder());
    }


}
