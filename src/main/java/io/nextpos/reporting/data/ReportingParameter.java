package io.nextpos.reporting.data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ReportingParameter {

    private LocalDateTime fromDate;

    private LocalDateTime toDate;
}
