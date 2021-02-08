package io.nextpos.calendarevent.web.model;

import io.nextpos.calendarevent.data.CalendarEvent;
import io.nextpos.calendarevent.data.CalendarEventSeries;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class CalendarEventResponse {

    private String id;

    private final CalendarEvent.EventType eventType;

    private final String eventName;

    private final CalendarEvent.EventOwner eventOwner;

    private final Map<String, List<CalendarEvent.EventResource>> eventResources;

    private List<CalendarEvent.EventResource> myEventResources;

    private final CalendarEvent.EventDetails eventDetails;

    private final CalendarEvent.EventStatus status;

    private final Date startTime;

    private final Date endTime;

    private final String eventColor;

    private String eventSeriesId;

    private CalendarEventSeries.EventRepeat eventRepeat;

    private Date repeatEndDate;

    public CalendarEventResponse(CalendarEvent calendarEvent) {
        id = calendarEvent.getId();
        eventType = calendarEvent.getEventType();
        eventName = calendarEvent.getEventName();
        eventOwner = calendarEvent.getEventOwner();
        eventResources = calendarEvent.getEventResources().stream()
                .collect(Collectors.groupingBy(e -> StringUtils.isNotBlank(e.getWorkingArea()) ? e.getWorkingArea() : "_default"));

        eventDetails = calendarEvent.getEventDetails();
        status = calendarEvent.getStatus();
        startTime = calendarEvent.getStartTime();
        endTime = calendarEvent.getEndTime();
        eventColor = calendarEvent.getEventColor();

        final CalendarEventSeries eventSeries = calendarEvent.getEventSeries();

        eventSeriesId = id;

        if (eventSeries != null) {
            eventSeriesId = eventSeries.getId();
            eventRepeat = eventSeries.getEventRepeat();
            repeatEndDate = eventSeries.getRepeatEndDate();
        }
    }
}
