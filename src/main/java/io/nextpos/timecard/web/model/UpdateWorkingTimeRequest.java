package io.nextpos.timecard.web.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UpdateWorkingTimeRequest {

    private int workingHours;

    private int workingMinutes;
}
