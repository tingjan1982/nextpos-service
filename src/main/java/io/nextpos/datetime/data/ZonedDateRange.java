package io.nextpos.datetime.data;

import io.nextpos.reporting.data.DateParameter;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
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

    public void updateDateRange(DateParameter dateParameter) {

        zonedFromDate = dateParameter.getFromDate().atZone(clientTimeZone);
        zonedToDate = dateParameter.getToDate().atZone(clientTimeZone);
    }

    public LocalDateTime getFromLocalDateTime() {
        return zonedFromDate.toLocalDateTime();
    }

    public LocalDateTime getToLocalDateTime() {
        return zonedToDate.toLocalDateTime();
    }

    public Date getFromDate() {
        return Date.from(zonedFromDate.toInstant());
    }

    public Date getToDate() {
        return Date.from(zonedToDate.toInstant());
    }

    /**
     * Bucket range requires the range to be m .. n + 1 (e.g. range of 3 => 1, 2, 3, 4)
     */
    public IntStream bucketDateRange() {

        final int fromDayOfYear = zonedFromDate.getDayOfYear();
        final int toDayOfYear = zonedToDate.getDayOfYear();

        if (fromDayOfYear > toDayOfYear) {
            return IntStream.concat(IntStream.rangeClosed(1, toDayOfYear), IntStream.rangeClosed(fromDayOfYear, 366));
        }

        return IntStream.rangeClosed(fromDayOfYear, toDayOfYear + 1);
    }

    public IntStream dateRange() {
        final int fromDayOfYear = zonedFromDate.getDayOfYear();
        final int toDayOfYear = zonedToDate.getDayOfYear();

        if (fromDayOfYear > toDayOfYear) {
            return IntStream.concat(IntStream.rangeClosed(1, toDayOfYear), IntStream.rangeClosed(fromDayOfYear, 365));
        }

        return IntStream.rangeClosed(fromDayOfYear, toDayOfYear);
    }
}
