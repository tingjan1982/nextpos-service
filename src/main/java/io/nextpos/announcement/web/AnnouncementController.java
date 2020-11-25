package io.nextpos.announcement.web;

import io.nextpos.announcement.data.Announcement;
import io.nextpos.announcement.service.AnnouncementService;
import io.nextpos.announcement.web.model.AnnouncementRequest;
import io.nextpos.announcement.web.model.AnnouncementResponse;
import io.nextpos.announcement.web.model.AnnouncementsResponse;
import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientObjectOwnershipService;
import io.nextpos.shared.web.ClientResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    private final ClientObjectOwnershipService clientObjectOwnershipService;

    @Autowired
    public AnnouncementController(final AnnouncementService announcementService, final ClientObjectOwnershipService clientObjectOwnershipService) {
        this.announcementService = announcementService;
        this.clientObjectOwnershipService = clientObjectOwnershipService;
    }

    @PostMapping
    public AnnouncementResponse createAnnouncement(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                   @Valid @RequestBody AnnouncementRequest announcementRequest) {

        Announcement announcement = fromRequest(client, announcementRequest);
        final Announcement savedAnnouncement = announcementService.saveAnnouncement(announcement);

        return toResponse(savedAnnouncement);
    }

    private Announcement fromRequest(final Client client, final AnnouncementRequest announcementRequest) {

        final Announcement announcement = new Announcement(client.getId(),
                announcementRequest.getTitleIcon(),
                announcementRequest.getTitle(),
                announcementRequest.getMarkdownContent());

        announcement.setExpireAt(announcementRequest.getExpireAt());
        announcement.setOrder(announcementRequest.getOrder());

        return announcement;
    }

    @GetMapping("/{id}")
    public AnnouncementResponse getAnnouncement(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                @PathVariable final String id) {

        final Announcement announcement = clientObjectOwnershipService.checkWithClientIdOwnership(client, () -> announcementService.getAnnouncement(id));

        return toResponse(announcement);
    }

    @GetMapping
    public AnnouncementsResponse getAnnouncements(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        final List<Announcement> announcements = announcementService.getAnnouncements(client.getId());

        final List<AnnouncementResponse> results = announcements.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new AnnouncementsResponse(results);
    }

    @PostMapping("/{id}")
    public AnnouncementResponse updateAnnouncement(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                   @PathVariable final String id,
                                                   @Valid @RequestBody AnnouncementRequest announcementRequest) {

        final Announcement announcement = clientObjectOwnershipService.checkWithClientIdOwnership(client, () -> announcementService.getAnnouncement(id));

        updateAnnouncementFromRequest(announcement, announcementRequest);

        return toResponse(announcementService.saveAnnouncement(announcement));
    }

    private void updateAnnouncementFromRequest(final Announcement announcement, final @Valid AnnouncementRequest announcementRequest) {

        announcement.setTitleIcon(announcementRequest.getTitleIcon());
        announcement.setTitleText(announcementRequest.getTitle());
        announcement.setMarkdownContent(announcementRequest.getMarkdownContent());
        announcement.setOrder(announcementRequest.getOrder());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAnnouncement(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                   @PathVariable final String id) {

        final Announcement announcement = clientObjectOwnershipService.checkWithClientIdOwnership(client, () -> announcementService.getAnnouncement(id));
        announcementService.deleteAnnouncement(announcement);
    }

    private AnnouncementResponse toResponse(final Announcement announcement) {

        return new AnnouncementResponse(announcement.getId(),
                announcement.getTitleIcon(),
                announcement.getTitleText(),
                announcement.getMarkdownContent(),
                announcement.getExpireAt(),
                announcement.getOrder());
    }


}
