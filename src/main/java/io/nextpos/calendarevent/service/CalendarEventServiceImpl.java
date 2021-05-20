package io.nextpos.calendarevent.service;

import io.nextpos.calendarevent.data.CalendarEvent;
import io.nextpos.calendarevent.data.CalendarEventRepository;
import io.nextpos.calendarevent.data.CalendarEventSeries;
import io.nextpos.calendarevent.data.CalendarEventSeriesRepository;
import io.nextpos.calendarevent.service.bean.EventRepeatObject;
import io.nextpos.calendarevent.service.bean.UpdateCalendarEventObject;
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

    private final EventSeriesCreator eventSeriesCreator;

    private final EventSeriesUpdater eventSeriesUpdater;

    private final CalendarEventRepository calendarEventRepository;

    private final CalendarEventSeriesRepository calendarEventSeriesRepository;

    private final MongoTemplate mongoTemplate;

    @Autowired
    public CalendarEventServiceImpl(EventSeriesCreator eventSeriesCreator, EventSeriesUpdater eventSeriesUpdater, CalendarEventRepository calendarEventRepository, CalendarEventSeriesRepository calendarEventSeriesRepository, MongoTemplate mongoTemplate) {
        this.eventSeriesCreator = eventSeriesCreator;
        this.eventSeriesUpdater = eventSeriesUpdater;
        this.calendarEventRepository = calendarEventRepository;
        this.calendarEventSeriesRepository = calendarEventSeriesRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<CalendarEvent> createCalendarEvent(CalendarEvent baseCalendarEvent, EventRepeatObject eventRepeatObj) {

        final CalendarEventSeries.EventRepeat eventRepeat = eventRepeatObj.getEventRepeat();

        if (eventRepeat == CalendarEventSeries.EventRepeat.NONE) {
            return List.of(calendarEventRepository.save(baseCalendarEvent));
        } else {
            return eventSeriesCreator.createEventSeriesEvent(baseCalendarEvent, eventRepeatObj);
        }
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
    public List<CalendarEvent> updateEventResources(CalendarEvent calendarEvent, List<CalendarEvent.EventResource> eventResources, boolean applyToSeries) {

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

            return seriesEvents;
        } else {
            return List.of(saveCalendarEvent(calendarEvent));
        }
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
    public List<CalendarEvent> updateCalendarEvent(CalendarEvent calendarEvent, UpdateCalendarEventObject updateCalendarEvent) {

        return eventSeriesUpdater.updateCalendarEvent(calendarEvent, updateCalendarEvent);
    }

    @Override
    public void deleteCalendarEvent(CalendarEvent calendarEvent, boolean applyToSeries) {
        final CalendarEventSeries eventSeries = calendarEvent.getEventSeries();

        if (eventSeries != null && applyToSeries) {
            final List<CalendarEvent> seriesEvents = calendarEventRepository.findAllByClientIdAndEventSeries_Id(calendarEvent.getClientId(), eventSeries.getId());

            seriesEvents.forEach(e -> {
                        if (e.isIsolated()) {
                            e.setEventSeries(null);
                            e.setIsolated(false);
                            calendarEventRepository.save(e);
                        } else {
                            calendarEventRepository.delete(e);
                        }
                    });

            calendarEventSeriesRepository.delete(eventSeries);

        } else {
            calendarEventRepository.delete(calendarEvent);
        }
    }
}
