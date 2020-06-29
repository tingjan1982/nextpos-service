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
        public DateParameter toReportingParameter(LocalDate date) {

            final LocalDateTime startOfDay = date.atStartOfDay();
            final LocalDateTime endOfDay = date.atTime(23, 59, 59);

            return new DateParameter(startOfDay, endOfDay);
        }
    },

    WEEK {
        @Override
        public DateParameter toReportingParameter(LocalDate date) {

//            final LocalDateTime startOfWeek = LocalDate.now().atStartOfDay().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
//            final LocalDateTime endOfWeek = startOfWeek.plusWeeks(1);

            final TemporalField temporalField = WeekFields.of(DayOfWeek.MONDAY, 7).dayOfWeek();
            final LocalDateTime firstDayOfWeek = date.with(temporalField, 1).atStartOfDay();
            final LocalDateTime lastDayOfWeek = date.with(temporalField, 7).atTime(23, 59, 59);

            return new DateParameter(firstDayOfWeek, lastDayOfWeek);
        }
    },

    MONTH {
        @Override
        public DateParameter toReportingParameter(LocalDate date) {

            final LocalDateTime startOfMonth = date.withDayOfMonth(1).atStartOfDay();
            final LocalDateTime endOfMonth = date.with(TemporalAdjusters.lastDayOfMonth()).atTime(23, 59, 59);

            return new DateParameter(startOfMonth, endOfMonth);
        }
    },

    CUSTOM {
        @Override
        public DateParameter toReportingParameter(LocalDate date) {
            throw new BusinessLogicException("Please specify fromDate and toDate parameters.");
        }
    },

    RANGE {
        @Override
        public DateParameter toReportingParameter(LocalDate date) {
            throw new BusinessLogicException("Please specify fromDate and toDate parameters.");
        }
    },

    SHIFT {
        @Override
        public DateParameter toReportingParameter(LocalDate date) {
            throw new UnsupportedOperationException("This shouldn't be called as this is handled in OrderController.");
        }
    };

    public abstract DateParameter toReportingParameter(LocalDate date);
}
