package io.nextpos.announcement.web;

import io.nextpos.announcement.data.GlobalAnnouncement;
import io.nextpos.announcement.service.GlobalAnnouncementService;
import io.nextpos.announcement.web.model.GlobalAnnouncementRequest;
import io.nextpos.announcement.web.model.GlobalAnnouncementResponse;
import io.nextpos.announcement.web.model.GlobalAnnouncementsResponse;
import io.nextpos.announcement.web.model.MarkAsReadRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/globalAnnouncements")
public class GlobalAnnouncementController {

    private final GlobalAnnouncementService globalAnnouncementService;

    @Autowired
    public GlobalAnnouncementController(GlobalAnnouncementService globalAnnouncementService) {
        this.globalAnnouncementService = globalAnnouncementService;
    }

    @PostMapping
    public GlobalAnnouncementResponse createGlobalAnnouncement(@Valid @RequestBody GlobalAnnouncementRequest request) {

        final GlobalAnnouncement globalAnnouncement = globalAnnouncementService.createGlobalAnnouncement(request.getTitle(), request.getContent());

        return new GlobalAnnouncementResponse(globalAnnouncement);
    }

    @GetMapping
    public GlobalAnnouncementsResponse getGlobalAnnouncements() {

        final List<GlobalAnnouncementResponse> results = globalAnnouncementService.getGlobalAnnouncements().stream()
                .map(GlobalAnnouncementResponse::new)
                .collect(Collectors.toList());

        return new GlobalAnnouncementsResponse(results);
    }

    @GetMapping("/{id}")
    public GlobalAnnouncementResponse getGlobalAnnouncement(@PathVariable String id) {

        final GlobalAnnouncement globalAnnouncement = globalAnnouncementService.getGlobalAnnouncement(id);

        return new GlobalAnnouncementResponse(globalAnnouncement);
    }

    @PostMapping("/{id}")
    public GlobalAnnouncementResponse updateGlobalAnnouncement(@PathVariable String id,
                                                               @Valid @RequestBody GlobalAnnouncementRequest request) {

        final GlobalAnnouncement globalAnnouncement = globalAnnouncementService.getGlobalAnnouncement(id);
        updateFromRequest(globalAnnouncement, request);

        globalAnnouncementService.saveGlobalAnnouncement(globalAnnouncement);
        return new GlobalAnnouncementResponse(globalAnnouncement);
    }

    private void updateFromRequest(GlobalAnnouncement globalAnnouncement, GlobalAnnouncementRequest request) {

        globalAnnouncement.setTitle(request.getTitle());
        globalAnnouncement.setContent(request.getContent());
        globalAnnouncement.clearReadDevices();
    }

    @PostMapping("/{id}/read")
    public GlobalAnnouncementResponse markAsRead(@PathVariable String id,
                                                 @Valid @RequestBody MarkAsReadRequest request) {

        final GlobalAnnouncement globalAnnouncement = globalAnnouncementService.getGlobalAnnouncement(id);
        globalAnnouncement.markAsRead(request.getClientId(), request.getDeviceId());
        globalAnnouncementService.saveGlobalAnnouncement(globalAnnouncement);

        return new GlobalAnnouncementResponse(globalAnnouncement);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGlobalAnnouncement(@PathVariable String id) {

        final GlobalAnnouncement globalAnnouncement = globalAnnouncementService.getGlobalAnnouncement(id);
        globalAnnouncementService.deleteGlobalAnnouncement(globalAnnouncement);
    }
}
