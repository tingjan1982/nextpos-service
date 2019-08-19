package io.nextpos.reporting.data;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;

public enum DateParameterType {
    TODAY {
        @Override
        public ReportDateParameter toReportingParameter() {

            final LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
            final LocalDateTime endOfDay = startOfDay.plusDays(1);

            return new ReportDateParameter(startOfDay, endOfDay);
        }
    },

    WEEK {
        @Override
        public ReportDateParameter toReportingParameter() {

            final LocalDateTime startOfWeek = LocalDate.now().atStartOfDay().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            final LocalDateTime endOfWeek = startOfWeek.plusWeeks(1);

            return new ReportDateParameter(startOfWeek, endOfWeek);
        }
    },

    MONTH {
        @Override
        public ReportDateParameter toReportingParameter() {

            final LocalDateTime startOfMonth = LocalDate.now().atStartOfDay().with(TemporalAdjusters.firstDayOfMonth());
            final LocalDateTime endOfMonth = startOfMonth.plusMonths(1);

            return new ReportDateParameter(startOfMonth, endOfMonth);
        }
    },

    RANGE {
        @Override
        public ReportDateParameter toReportingParameter() {
            throw new UnsupportedOperationException("The parameterized variant of this method should be used instead.");
        }
    };

    public abstract ReportDateParameter toReportingParameter();

    public ReportDateParameter toReportingParameter(Date fromDate, Date toDate) {

        if (fromDate != null && toDate != null) {
            final LocalDateTime fromDT = LocalDate.ofInstant(fromDate.toInstant(), ZoneId.systemDefault()).atStartOfDay();
            final LocalDateTime toDT = LocalDate.ofInstant(toDate.toInstant(), ZoneId.systemDefault()).atStartOfDay().plusDays(1);

            return new ReportDateParameter(fromDT, toDT);
        }

        return TODAY.toReportingParameter();
    }
}
