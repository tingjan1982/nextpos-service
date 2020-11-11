package io.nextpos.calendarevent.data;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;
import java.util.List;

public interface CalendarEventRepository extends MongoRepository<CalendarEvent, String> {

    List<CalendarEvent> findAllByClientIdAndEventOwner_OwnerIdAndEventOwner_OwnerType(String clientId, String eventOwnerId, CalendarEvent.OwnerType ownerType);

    List<CalendarEvent> findAllByClientIdAndEventTypeAndEventResourcesContainingAndStartTimeBetween(String clientId, CalendarEvent.EventType eventType, CalendarEvent.EventResource eventResource, Date from, Date to);
}
