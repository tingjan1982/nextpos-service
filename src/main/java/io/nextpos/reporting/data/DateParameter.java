package io.nextpos.reporting.data;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Data
@RequiredArgsConstructor
public class DateParameter {

    private final LocalDateTime fromDate;

    private final LocalDateTime toDate;
}
