package io.nextpos.announcement.web.model;


import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.util.Date;

@Data
@NoArgsConstructor
public class AnnouncementRequest {

    private String titleIcon;

    @NotBlank
    private String title;

    @NotBlank
    private String markdownContent;

    private Date expireAt;

    private int order;
}
