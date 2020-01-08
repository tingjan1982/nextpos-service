package io.nextpos.announcement.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AnnouncementsResponse {

    private List<AnnouncementResponse> results;
}
