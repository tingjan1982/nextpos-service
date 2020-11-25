package io.nextpos.announcement.web.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
public class GlobalAnnouncementRequest {

    @NotBlank
    private String title;

    private String content;
}
