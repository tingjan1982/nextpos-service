package io.nextpos.calendarevent.service;

import io.nextpos.calendarevent.data.CalendarEvent;
import io.nextpos.calendarevent.data.CalendarEventRepository;
import io.nextpos.client.data.Client;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
@ChainedTransaction
public class CalendarEventServiceImpl implements CalendarEventService {

    private final CalendarEventRepository calendarEventRepository;

    private final MongoTemplate mongoTemplate;

    @Autowired
    public CalendarEventServiceImpl(CalendarEventRepository calendarEventRepository, MongoTemplate mongoTemplate) {
        this.calendarEventRepository = calendarEventRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public CalendarEvent saveCalendarEvent(CalendarEvent calendarEvent) {
        return calendarEventRepository.save(calendarEvent);
    }

    @Override
    public CalendarEvent getCalendarEvent(String id) {
        return calendarEventRepository.findById(id).orElseThrow(() -> {
            throw new ObjectNotFoundException(id, CalendarEvent.class);
        });
    }

    @Override
    public CalendarEvent addEventResource(CalendarEvent calendarEvent, CalendarEvent.EventResource eventResource) {

        calendarEvent.addEventSource(eventResource);
        return saveCalendarEvent(calendarEvent);
    }

    @Override
    public CalendarEvent removeEventResource(CalendarEvent calendarEvent, CalendarEvent.EventResource eventResource) {

        calendarEvent.removeEventResource(eventResource);
        return saveCalendarEvent(calendarEvent);
    }

    @Override
    public List<CalendarEvent> getCalendarEventsForEventOwner(String clientId, String eventOwnerId, CalendarEvent.OwnerType ownerType) {

        return calendarEventRepository.findAllByClientIdAndEventOwner_OwnerIdAndEventOwner_OwnerType(clientId, eventOwnerId, ownerType);
    }

    @Override
    public List<CalendarEvent> getCalendarEventsForEventResource(Client client, Date from, Date to, CalendarEvent.EventType eventType, CalendarEvent.EventResource eventResource) {

        return calendarEventRepository.findAllByClientIdAndEventTypeAndEventResourcesContainingAndStartTimeBetween(
                client.getId(),
                eventType,
                eventResource,
                from,
                to);
    }

    @Override
    public void deleteCalendarEvents(String clientId, String eventOwnerId, CalendarEvent.OwnerType ownerType) {

        Query query = Query.query(where("clientId").is(clientId).and("eventOwner.ownerId").is(eventOwnerId).and("eventOwner.ownerType").is(ownerType));
        mongoTemplate.remove(query, CalendarEvent.class);
    }
}
