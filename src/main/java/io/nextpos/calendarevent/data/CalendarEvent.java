package io.nextpos.calendarevent.data;

import io.nextpos.membership.data.Membership;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.shared.model.MongoBaseObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

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

    private EventResource eventResource;

    private EventStatus status;

    private Date startTime;

    /**
     * optional
     */
    private Date endTime;

    public CalendarEvent(String clientId, EventType eventType, String eventName, Date startTime, Date endTime) {
        this.clientId = clientId;
        this.eventType = eventType;
        this.eventName = eventName;
        this.startTime = startTime;
        this.endTime = endTime;

        this.eventOwner = new EventOwner();
        this.eventResource = new EventResource();
        this.status = EventStatus.PLANNED;
    }

    public void updateEventSource(EventResource eventResource) {
        setEventResource(eventResource);
    }

    public enum EventType {

        ROSTER, RESERVATION
    }

    @Data
    @NoArgsConstructor
    public static class EventOwner {

        private String name;

        private String phoneNumber;

        @DBRef
        private Membership membership;

        @DBRef
        private Order order;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventResource {

        /**
         * Can point to table id, product id or staff id
         */
        private String resourceId;

        private ResourceType resourceType;

        private String resourceName;
    }

    public enum ResourceType {

        TABLE, PRODUCT, STAFF
    }

    public enum EventStatus {

        PLANNED,

        CONFIRMED,

        ATTENDED,

        CANCELLED
    }
}
