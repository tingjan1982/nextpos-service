package io.nextpos.reporting.data;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.stream.IntStream;

/**
 * Zoned date fields are used to store the date range with zone information.
 * The UTC methods are used to convert zoned datetime to UTC for MongoDB search.
 */
@Data
@RequiredArgsConstructor
public class ZonedDateRange {

    private final ZoneId clientTimeZone;

    private ZonedDateTime zonedFromDate;

    private ZonedDateTime zonedToDate;

    public LocalDateTime getFromLocalDateTime() {
        return zonedFromDate.toLocalDateTime();
    }

    public LocalDateTime getToLocalDateTime() {
        return zonedToDate.toLocalDateTime();
    }

    public IntStream bucketDateRange() {
        return IntStream.rangeClosed(zonedFromDate.getDayOfYear(), zonedToDate.getDayOfYear());
    }

    public IntStream dateRange() {
        return IntStream.rangeClosed(zonedFromDate.getDayOfYear(), zonedToDate.getDayOfYear() - 1);
    }
}
