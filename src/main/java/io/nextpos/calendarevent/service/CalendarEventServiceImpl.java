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

import java.time.LocalDateTime;
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
    public CalendarEvent updateSelfEventResources(CalendarEvent calendarEvent, String resourceId, List<CalendarEvent.EventResource> eventResources) {

        calendarEvent.getEventResources().removeIf(er -> er.getResourceId().equals(resourceId));

        eventResources.forEach(calendarEvent::addEventSource);

        return saveCalendarEvent(calendarEvent);
    }

    @Override
    public CalendarEvent updateEventResources(CalendarEvent calendarEvent, List<CalendarEvent.EventResource> eventResources, boolean applyToSeries) {

        calendarEvent.removeAllEventResources();
        eventResources.forEach(calendarEvent::addEventSource);
        final CalendarEventSeries eventSeries = calendarEvent.getEventSeries();

        if (eventSeries != null && applyToSeries) {
            final List<CalendarEvent> seriesEvents = calendarEventRepository.findAllByClientIdAndEventSeries_Id(calendarEvent.getClientId(), eventSeries.getId());
            seriesEvents.forEach(e -> {
                e.removeAllEventResources();
                eventResources.forEach(e::addEventSource);
                this.saveCalendarEvent(e);
            });
        }

        return saveCalendarEvent(calendarEvent);
    }

    @Override
    public List<CalendarEvent> getCalendarEvents(String clientId, CalendarEvent.EventType eventType, Date from, Date to) {
        return calendarEventRepository.findAllByClientIdAndEventTypeAndStartTimeBetween(clientId, eventType, from, to);
    }

    @Override
    public List<CalendarEvent> getCalendarEvents(String clientId, CalendarEvent.EventType eventType, CalendarEvent.EventResource eventResource, Date from, Date to) {

        Query query = Query.query(where("clientId").is(clientId)
                .and("eventType").is(eventType)
                .and("eventResources").elemMatch(where("resourceId").is(eventResource.getResourceId()))
                .and("startTime").gte(from).lte(to));

        return mongoTemplate.find(query, CalendarEvent.class);
    }

    @Override
    public CalendarEvent updateCalendarEvent(CalendarEvent calendarEvent, LocalDateTime startTime, LocalDateTime endTime, long daysDiff, boolean applyToSeries) {

        final CalendarEventSeries eventSeries = calendarEvent.getEventSeries();

        if (eventSeries != null) {
            if (applyToSeries) {
                final List<CalendarEvent> seriesEvents = calendarEventRepository.findAllByClientIdAndEventSeries_Id(calendarEvent.getClientId(), eventSeries.getId());
                seriesEvents.forEach(e -> {
                    e.update(calendarEvent, startTime.toLocalTime(), endTime.toLocalTime(), daysDiff);
                    this.saveCalendarEvent(e);
                });
            } else {
                final boolean dateChanged = !calendarEvent.getStartTime().toInstant().equals(startTime.atZone(calendarEvent.getZoneId()).toInstant());

                if (dateChanged) {
                    calendarEvent.setEventSeries(null);
                }

                this.saveCalendarEvent(calendarEvent);
            }

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
