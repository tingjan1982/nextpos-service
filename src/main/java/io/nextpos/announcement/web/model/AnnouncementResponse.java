package io.nextpos.announcement.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AnnouncementResponse {

    private String id;

    private String titleIcon;

    private String title;

    private String markdownContent;

    private int order;
}
