package io.nextpos.reporting.data;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

// todo: rename this class to not be report specific. eg. DateParameters.
@Data
@RequiredArgsConstructor
public class ReportDateParameter {

    private final LocalDateTime fromDate;

    private final LocalDateTime toDate;
}
