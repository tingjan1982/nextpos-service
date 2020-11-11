package io.nextpos.calendarevent.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CalendarEventsResponse {

    private List<CalendarEventResponse> results;
}
