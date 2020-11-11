package io.nextpos.calendarevent.data;

import io.nextpos.shared.model.MongoBaseObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
public class CalendarEvent extends MongoBaseObject {

    @Id
    private String id;

    private String clientId;

    private EventType eventType;

    private String eventName;

    private EventOwner eventOwner;

    private List<EventResource> eventResources = new ArrayList<>();

    private EventDetails eventDetails = new EventDetails();

    private EventStatus status = EventStatus.PLANNED;

    private Date startTime;

    /**
     * optional
     */
    private Date endTime;

    public CalendarEvent(String clientId, EventType eventType, String eventName, EventOwner eventOwner, Date startTime, Date endTime) {
        this.clientId = clientId;
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
        ROSTER, CUSTOMER
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    public static class EventResource {

        /**
         * Can point to table id, product id or staff id
         */
        @EqualsAndHashCode.Include
        private String resourceId;

        @EqualsAndHashCode.Include
        private ResourceType resourceType;

        private String resourceName;
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
