package io.nextpos.calendarevent.web.model;

import io.nextpos.calendarevent.data.CalendarEvent;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class CalendarEventsResponse {

    private List<CalendarEventResponse> results;

    public CalendarEventsResponse(List<CalendarEvent> calendarEvents) {
        results = calendarEvents.stream()
                .map(CalendarEventResponse::new)
                .collect(Collectors.toList());
    }
}
