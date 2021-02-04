package io.nextpos.calendarevent.service.bean;

import io.nextpos.calendarevent.data.CalendarEventSeries;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class EventRepeatObject {

    private CalendarEventSeries.EventRepeat eventRepeat;

    private LocalDateTime repeatEndDate;

    public static EventRepeatObject eventRepeat(CalendarEventSeries.EventRepeat eventRepeat) {
        return new EventRepeatObject(eventRepeat, null);
    }

    public static EventRepeatObject none() {
        return eventRepeat(CalendarEventSeries.EventRepeat.NONE);
    }
}
