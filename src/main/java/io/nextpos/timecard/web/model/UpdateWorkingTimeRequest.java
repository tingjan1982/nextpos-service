package io.nextpos.timecard.web.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class UpdateWorkingTimeRequest {

    private LocalDateTime clockIn;

    private LocalDateTime clockOut;

    private int workingHours;

    private int workingMinutes;
}
