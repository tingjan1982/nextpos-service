package io.nextpos.roster.web.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class UpdateRosterEventRequest {

    @NotBlank
    private String eventName;

    @NotNull
    private LocalDateTime startTime;

    @NotNull
    private LocalDateTime endTime;

    private String eventColor;

    private Map<String, List<String>> workingAreaToUsernames;

    private boolean applyToSeries;
}
