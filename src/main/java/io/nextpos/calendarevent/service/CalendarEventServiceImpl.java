package io.nextpos.calendarevent.service;

import io.nextpos.calendarevent.data.CalendarEvent;
import io.nextpos.calendarevent.data.CalendarEventRepository;
import io.nextpos.calendarevent.data.CalendarEventSeries;
import io.nextpos.calendarevent.data.CalendarEventSeriesRepository;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.Date;
import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
@ChainedTransaction
public class CalendarEventServiceImpl implements CalendarEventService {

    private final CalendarEventRepository calendarEventRepository;

    private final CalendarEventSeriesRepository calendarEventSeriesRepository;

    private final MongoTemplate mongoTemplate;

    @Autowired
    public CalendarEventServiceImpl(CalendarEventRepository calendarEventRepository, CalendarEventSeriesRepository calendarEventSeriesRepository, MongoTemplate mongoTemplate) {
        this.calendarEventRepository = calendarEventRepository;
        this.calendarEventSeriesRepository = calendarEventSeriesRepository;
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
    public CalendarEvent updateEventResources(CalendarEvent calendarEvent, List<CalendarEvent.EventResource> eventResources) {

        calendarEvent.removeAllEventResources();
        eventResources.forEach(er -> this.addEventResource(calendarEvent, er));

        return saveCalendarEvent(calendarEvent);
    }

    @Override
    public List<CalendarEvent> getCalendarEvents(String clientId, CalendarEvent.EventType eventType, Date from, Date to) {
        return calendarEventRepository.findAllByClientIdAndEventTypeAndStartTimeBetween(clientId, eventType, from, to);
    }

    @Deprecated
    @Override
    public List<CalendarEvent> getCalendarEventsForEventOwner(String clientId, String eventOwnerId, CalendarEvent.OwnerType ownerType) {

        return calendarEventRepository.findAllByClientIdAndEventOwner_OwnerIdAndEventOwner_OwnerType(clientId, eventOwnerId, ownerType);
    }

    @Override
    public List<CalendarEvent> getCalendarEvents(String clientId, CalendarEvent.EventType eventType, CalendarEvent.EventResource eventResource, Date from, Date to) {

        Query query = Query.query(where("clientId").is(clientId)
                .and("eventType").is(eventType)
                .and("eventResources").elemMatch(where("resourceId").is(eventResource.getResourceId()))
                .and("startTime").gte(from).lte(to));

        return mongoTemplate.find(query, CalendarEvent.class);
    }

    @Deprecated
    @Override
    public void deleteCalendarEvents(String clientId, String eventOwnerId, CalendarEvent.OwnerType ownerType) {

        Query query = Query.query(where("clientId").is(clientId)
                .and("eventOwner.ownerId").is(eventOwnerId)
                .and("eventOwner.ownerType").is(ownerType));

        mongoTemplate.remove(query, CalendarEvent.class);
    }

    @Override
    public CalendarEvent updateCalendarEvent(CalendarEvent calendarEvent, LocalTime startTime, LocalTime endTime, long daysDiff, boolean applyToSeries) {

        final CalendarEventSeries eventSeries = calendarEvent.getEventSeries();

        if (eventSeries != null && applyToSeries) {
            final List<CalendarEvent> seriesEvents = calendarEventRepository.findAllByClientIdAndEventSeries_Id(calendarEvent.getClientId(), eventSeries.getId());
            seriesEvents.forEach(e -> {
                e.update(calendarEvent, startTime, endTime, daysDiff);
                this.saveCalendarEvent(e);
            });

        } else {
            this.saveCalendarEvent(calendarEvent);
        }

        return calendarEvent;
    }

    @Override
    public void deleteCalendarEvent(CalendarEvent calendarEvent, boolean applyToSeries) {
        final CalendarEventSeries eventSeries = calendarEvent.getEventSeries();

        if (eventSeries != null && applyToSeries) {
            final List<CalendarEvent> seriesEvents = calendarEventRepository.findAllByClientIdAndEventSeries_Id(calendarEvent.getClientId(), eventSeries.getId());
            seriesEvents.forEach(calendarEventRepository::delete);

            calendarEventSeriesRepository.delete(eventSeries);

        } else {
            calendarEventRepository.delete(calendarEvent);
        }
    }

    @Override
    public CalendarEventSeries saveCalendarEventSeries(CalendarEventSeries calendarEventSeries) {
        return calendarEventSeriesRepository.save(calendarEventSeries);
    }
}
