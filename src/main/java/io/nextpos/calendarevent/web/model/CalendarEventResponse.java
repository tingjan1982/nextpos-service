package io.nextpos.calendarevent.web.model;

import io.nextpos.calendarevent.data.CalendarEvent;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class CalendarEventResponse {

    private String id;

    private final CalendarEvent.EventType eventType;

    private final CalendarEvent.EventOwner eventOwner;

    private final List<CalendarEvent.EventResource> eventResources;

    private final CalendarEvent.EventDetails eventDetails;

    private final CalendarEvent.EventStatus status;

    private final Date startTime;

    private final Date endTime;


    public CalendarEventResponse(CalendarEvent calendarEvent) {
        id = calendarEvent.getId();
        eventType = calendarEvent.getEventType();
        eventOwner = calendarEvent.getEventOwner();
        eventResources = calendarEvent.getEventResources();
        eventDetails = calendarEvent.getEventDetails();
        status = calendarEvent.getStatus();
        startTime = calendarEvent.getStartTime();
        endTime = calendarEvent.getEndTime();
    }

}
