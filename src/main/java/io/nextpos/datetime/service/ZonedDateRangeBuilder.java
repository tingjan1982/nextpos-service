package io.nextpos.datetime.service;

import io.nextpos.client.data.Client;
import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.ordermanagement.data.Shift;
import io.nextpos.reporting.data.DateParameter;
import io.nextpos.reporting.data.DateParameterType;
import io.nextpos.shared.exception.BusinessLogicException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class ZonedDateRangeBuilder {

    private final Client client;

    private final DateParameterType dateParameterType;

    private Shift shift;

    private LocalDate date;

    private LocalDateTime fromDate;

    private LocalDateTime toDate;

    private ZonedDateRangeBuilder(Client client, DateParameterType dateParameterType) {
        this.client = client;
        this.dateParameterType = dateParameterType;
    }

    public static ZonedDateRangeBuilder builder(Client client, DateParameterType dateParameterType) {
        return new ZonedDateRangeBuilder(client, dateParameterType);
    }

    public ZonedDateRangeBuilder shift(Shift shift) {
        this.shift = shift;
        return this;
    }

    public ZonedDateRangeBuilder date(LocalDate date) {
        this.date = date;
        return this;
    }

    public ZonedDateRangeBuilder dateRange(LocalDateTime fromDate, LocalDateTime toDate) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        return this;
    }

    public ZonedDateRange build() {

        DateParameter dateParameter = null;

        switch (dateParameterType) {
            case TODAY:
            case WEEK:
            case MONTH:
                final LocalDate date = this.date != null ? this.date : LocalDate.now();
                dateParameter = dateParameterType.toReportingParameter(date);
                break;

            case SHIFT:
                if (shift == null) {
                    throw new BusinessLogicException("A valid shift needs to be provided");
                }

                final ZoneId zoneId = client.getZoneId();
                dateParameter = new DateParameter(shift.getStart().toLocalDateTime(zoneId), shift.getEnd().toLocalDateTime(zoneId));
                break;

            case CUSTOM:
            case RANGE:

                if (fromDate == null || toDate == null) {
                    throw new BusinessLogicException("from date and to date need to be provided");
                }

                dateParameter = new DateParameter(fromDate, toDate);
                break;
        }

        if (dateParameter == null) {
            throw new BusinessLogicException("Date parameter has not been initialized properly.");
        }

        final ZonedDateRange zonedDateRange = new ZonedDateRange(client.getZoneId());
        zonedDateRange.updateDateRange(dateParameter);

        return zonedDateRange;
    }
}
