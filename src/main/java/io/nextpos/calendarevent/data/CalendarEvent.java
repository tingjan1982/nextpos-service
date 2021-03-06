package io.nextpos.calendarevent.data;

import io.nextpos.calendarevent.service.bean.UpdateCalendarEventObject;
import io.nextpos.shared.model.MongoBaseObject;
import io.nextpos.shared.model.WithClientId;
import io.nextpos.shared.util.DateTimeUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
public class CalendarEvent extends MongoBaseObject implements WithClientId {

    @Id
    private String id;

    private String clientId;

    private ZoneId zoneId;

    private EventType eventType;

    private String eventName;

    private EventOwner eventOwner;

    private List<EventResource> eventResources = new ArrayList<>();

    private EventDetails eventDetails = new EventDetails();

    private EventStatus status = EventStatus.PLANNED;

    private Date startTime;

    private Date endTime;

    private String eventColor;

    /**
     * Flag to indicate that this event has date changed and therefore its date and resources
     * will not be changed further as part of event series change.
     */
    private boolean isolated;

    @DBRef
    private CalendarEventSeries eventSeries;

    public CalendarEvent(String clientId, ZoneId zoneId, EventType eventType, String eventName, EventOwner eventOwner, Date startTime, Date endTime) {
        this.id = ObjectId.get().toString();
        this.clientId = clientId;
        this.zoneId = zoneId;
        this.eventType = eventType;
        this.eventName = eventName;
        this.eventOwner = eventOwner;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public void addEventSource(EventResource eventResource) {
        if (!eventResources.contains(eventResource)) {
            this.eventResources.add(eventResource);
        }
    }

    public void removeEventResource(EventResource eventResource) {
        this.eventResources.remove(eventResource);
    }

    public void removeAllEventResources() {
        this.eventResources.clear();
    }

    public CalendarEvent copy(LocalDateTime startTime, LocalDateTime endTime) {
        return copy(DateTimeUtil.toDate(zoneId, startTime), DateTimeUtil.toDate(zoneId, endTime));
    }

    public CalendarEvent copy(Date startTime, Date endTime) {

        final CalendarEvent copy = new CalendarEvent(this.clientId,
                this.zoneId,
                this.eventType,
                this.eventName,
                this.eventOwner,
                startTime,
                endTime);

        copy.setEventColor(this.eventColor);
        copy.setEventResources(this.eventResources);
        copy.setEventSeries(this.eventSeries);

        return copy;
    }

    public void update(CalendarEvent calendarEvent, UpdateCalendarEventObject updateCalendarEvent) {

        setEventName(calendarEvent.getEventName());
        setEventColor(calendarEvent.getEventColor());

        if (isolated) {
            // when date is marked as updated, do not update start and end time and resources.
            return;
        }

        long daysDiff = updateCalendarEvent.getDaysDiff();
        LocalDate startDate = this.getStartTime().toInstant().atZone(zoneId).toLocalDate().plusDays(daysDiff);
        LocalDateTime startDt = LocalDateTime.of(startDate, updateCalendarEvent.getStartTime().toLocalTime());
        LocalDate endDate = this.getStartTime().toInstant().atZone(zoneId).toLocalDate().plusDays(daysDiff);
        LocalDateTime endDt = LocalDateTime.of(endDate, updateCalendarEvent.getEndTime().toLocalTime());

        if (endDt.compareTo(startDt) <= 0) {
            endDt = endDt.plusDays(1);
        }

        setStartTime(DateTimeUtil.toDate(zoneId, startDt));
        setEndTime(DateTimeUtil.toDate(zoneId, endDt));

        if (updateCalendarEvent.getEventResources() != null) {
            this.removeAllEventResources();
            updateCalendarEvent.getEventResources().forEach(this::addEventSource);
        }
    }

    public LocalDate getLocalStartDate() {
        return DateTimeUtil.toLocalDate(zoneId, startTime);
    }

    public enum EventType {

        ROSTER, RESERVATION
    }

    @Data
    @NoArgsConstructor
    public static class EventOwner {

        private String ownerId;

        private String ownerName;

        private OwnerType ownerType;

        public static EventOwner createWithOwnerId(String ownerId, OwnerType ownerType) {
            final EventOwner eventOwner = new EventOwner();
            eventOwner.setOwnerId(ownerId);
            eventOwner.setOwnerType(ownerType);

            return eventOwner;
        }

        public static EventOwner createWithOwnerName(String ownerName, OwnerType ownerType) {
            final EventOwner eventOwner = new EventOwner();
            eventOwner.setOwnerName(ownerName);
            eventOwner.setOwnerType(ownerType);

            return eventOwner;
        }
    }

    public enum OwnerType {

        CUSTOMER,
        MEMBER,
        STAFF
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    public static class EventResource {

        /**
         * Can point to table id, product id or user id
         */
        @EqualsAndHashCode.Include
        private String resourceId;

        @EqualsAndHashCode.Include
        private ResourceType resourceType;

        private String resourceName;

        @EqualsAndHashCode.Include
        private String workingArea;

        public EventResource(String resourceId, ResourceType resourceType) {
            this.resourceId = resourceId;
            this.resourceType = resourceType;
        }
    }

    public enum ResourceType {

        TABLE, PRODUCT, STAFF
    }

    @Data
    public static class EventDetails {

        private String phoneNumber;

        private int peopleCount;

        private String note;
    }

    public enum EventStatus {

        /**
         * Default state.
         */
        PLANNED,

        /**
         * Event resource has been allocated.
         */
        ALLOCATED,

        ATTENDED,

        CANCELLED

    }
}
