package io.nextpos.reporting.data;

import io.nextpos.shared.exception.BusinessLogicException;
import org.springframework.lang.NonNull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

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
            throw new BusinessLogicException("Please specify fromDate and toDate parameters.");
        }
    },

    SHIFT {
        @Override
        public ReportDateParameter toReportingParameter() {
            throw new UnsupportedOperationException("This shouldn't be called as this is handled in OrderController.");
        }
    };

    public abstract ReportDateParameter toReportingParameter();

    public static ReportDateParameter toReportingParameter(@NonNull DateParameterType dateParameterType, LocalDateTime fromDate, LocalDateTime toDate) {

        if (dateParameterType == RANGE && fromDate != null && toDate != null) {
            return new ReportDateParameter(fromDate, toDate);
        }

        return dateParameterType.toReportingParameter();
    }
}
