package io.nextpos.calendarevent.web.model;

import io.nextpos.calendarevent.data.CalendarEvent;
import io.nextpos.shared.util.DateTimeUtil;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class CalendarEventsResponse {

    private List<CalendarEventResponse> results;

    private Map<LocalDate, List<ResourceEventsResponse>> groupedResults;

    public CalendarEventsResponse(ZoneId zoneId, List<CalendarEvent> calendarEvents) {
        results = calendarEvents.stream()
                .map(CalendarEventResponse::new)
                .collect(Collectors.toList());

        final Map<LocalDate, Map<String, List<SingleResourceEvent>>> grouped = calendarEvents.stream()
                .map(e -> {
                    final LocalDate date = DateTimeUtil.toLocalDateTime(zoneId, e.getStartTime()).toLocalDate();
                    return e.getEventResources().stream()
                            .map(er -> new SingleResourceEvent(date, er.getResourceId(), new CalendarEventResponse(e)))
                            .collect(Collectors.toList());
                })
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(SingleResourceEvent::getDate, Collectors.groupingBy(SingleResourceEvent::getResource)));

        groupedResults = grouped.entrySet().stream()
                .map(e1 -> {
                    final List<ResourceEventsResponse> res = e1.getValue().entrySet().stream()
                            .map(e2 -> {
                                final List<CalendarEventResponse> events = e2.getValue().stream()
                                        .map(SingleResourceEvent::getEvent)
                                        .collect(Collectors.toList());
                                return new ResourceEventsResponse(e2.getKey(), events);
                            })
                            .collect(Collectors.toList());

                    return Map.entry(e1.getKey(), res);
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Data
    @AllArgsConstructor
    private static class SingleResourceEvent {

        private LocalDate date;

        private String resource;

        private CalendarEventResponse event;
    }

    @Data
    @AllArgsConstructor
    private static class ResourceEventsResponse {

        private String resource;

        private List<CalendarEventResponse> events;
    }
}
