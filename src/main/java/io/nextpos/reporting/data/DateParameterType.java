package io.nextpos.reporting.data;

import io.nextpos.shared.exception.BusinessLogicException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;

public enum DateParameterType {
    TODAY {
        @Override
        public ReportDateParameter toReportingParameter(LocalDate date) {

            final LocalDateTime startOfDay = date.atStartOfDay();
            final LocalDateTime endOfDay = date.atTime(23, 59, 59);

            return new ReportDateParameter(startOfDay, endOfDay);
        }
    },

    WEEK {
        @Override
        public ReportDateParameter toReportingParameter(LocalDate date) {

//            final LocalDateTime startOfWeek = LocalDate.now().atStartOfDay().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
//            final LocalDateTime endOfWeek = startOfWeek.plusWeeks(1);

            final TemporalField temporalField = WeekFields.of(DayOfWeek.MONDAY, 7).dayOfWeek();
            final LocalDateTime firstDayOfWeek = date.with(temporalField, 1).atStartOfDay();
            final LocalDateTime lastDayOfWeek = date.with(temporalField, 7).atTime(23, 59, 59);

            return new ReportDateParameter(firstDayOfWeek, lastDayOfWeek);
        }
    },

    MONTH {
        @Override
        public ReportDateParameter toReportingParameter(LocalDate date) {

            final LocalDateTime startOfMonth = date.withDayOfMonth(1).atStartOfDay();
            final LocalDateTime endOfMonth = date.with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59);

            return new ReportDateParameter(startOfMonth, endOfMonth);
        }
    },

    CUSTOM {
        @Override
        public ReportDateParameter toReportingParameter(LocalDate date) {
            throw new BusinessLogicException("Please specify fromDate and toDate parameters.");
        }
    },

    RANGE {
        @Override
        public ReportDateParameter toReportingParameter(LocalDate date) {
            throw new BusinessLogicException("Please specify fromDate and toDate parameters.");
        }
    },

    SHIFT {
        @Override
        public ReportDateParameter toReportingParameter(LocalDate date) {
            throw new UnsupportedOperationException("This shouldn't be called as this is handled in OrderController.");
        }
    };

    public abstract ReportDateParameter toReportingParameter(LocalDate date);

    // todo: remove this
    public static ReportDateParameter toReportingParameter(DateParameterType dateParameterType, LocalDateTime fromDate, LocalDateTime toDate) {

        if ((dateParameterType == RANGE || dateParameterType == CUSTOM) && fromDate != null && toDate != null) {
            return new ReportDateParameter(fromDate, toDate);
        }

        return dateParameterType.toReportingParameter(LocalDate.now());
    }
}
