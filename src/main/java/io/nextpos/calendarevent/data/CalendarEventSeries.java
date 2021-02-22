package io.nextpos.calendarevent.data;

import io.nextpos.shared.model.MongoBaseObject;
import io.nextpos.shared.util.DateTimeUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CalendarEventSeries extends MongoBaseObject {

    @Id
    private String id;

    private String clientId;

    private ZoneId zoneId;

    private YearMonth yearMonth;

    private Date seriesStartDate;

    private EventRepeat eventRepeat;

    private Date repeatEndDate;

    public CalendarEventSeries(CalendarEvent calendarEvent, EventRepeat eventRepeat, LocalDateTime repeatEndDate) {
        this.clientId = calendarEvent.getClientId();
        this.zoneId = calendarEvent.getZoneId();
        this.yearMonth = YearMonth.from(repeatEndDate);
        this.seriesStartDate = calendarEvent.getStartTime();
        this.eventRepeat = eventRepeat;
        this.repeatEndDate = DateTimeUtil.toDate(zoneId, repeatEndDate);
    }

    public List<LocalDate> updateAndGetSeriesDates(LocalDateTime startDate) {

        LocalDateTime startDateToUse = startDate;

        if (startDate.getMonth() != yearMonth.getMonth()) {
            startDateToUse = startDate.withMonth(yearMonth.getMonthValue());
        }

        seriesStartDate = DateTimeUtil.toDate(zoneId, startDateToUse);

        if (eventRepeat == EventRepeat.DAILY) {
            return startDate.toLocalDate().datesUntil(DateTimeUtil.toLocalDate(zoneId, repeatEndDate).plusDays(1))
                    .collect(Collectors.toList());
        } else {
            List<LocalDate> dates = new ArrayList<>();
            dates.add(startDate.toLocalDate());
            LocalDateTime nextStartTime = startDate;
            LocalDateTime localRepeatEndDate = DateTimeUtil.toLocalDateTime(zoneId, repeatEndDate);
            LocalDateTime lastDayOfTheSeries = nextStartTime.with(TemporalAdjusters.lastInMonth(nextStartTime.getDayOfWeek()));

            while (nextStartTime.compareTo(lastDayOfTheSeries) < 0 && nextStartTime.compareTo(localRepeatEndDate) < 0) {
                nextStartTime = nextStartTime.with(TemporalAdjusters.next(nextStartTime.getDayOfWeek()));
                dates.add(nextStartTime.toLocalDate());
            }

            return dates;
        }
    }

    public LocalDateTime localRepeatEndDate() {
        return DateTimeUtil.toLocalDateTime(zoneId, repeatEndDate);
    }

    public enum EventRepeat {
        NONE, DAILY, WEEKLY
    }
}
