package io.nextpos.announcement.web.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
public class MarkAsReadRequest {

    @NotBlank
    private String clientId;

    @NotBlank
    private String deviceId;
}
