package io.nextpos.calendarevent.data;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;
import java.util.List;

public interface CalendarEventRepository extends MongoRepository<CalendarEvent, String> {

    List<CalendarEvent> findAllByClientIdAndEventTypeAndEventResource_ResourceIdAndEventResource_ResourceTypeAndStartTimeBetween(String clientId, CalendarEvent.EventType eventType, String resourceId, CalendarEvent.ResourceType resourceType, Date from, Date to);
}
