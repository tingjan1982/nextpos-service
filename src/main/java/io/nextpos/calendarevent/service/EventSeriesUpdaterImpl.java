package io.nextpos.calendarevent.service;

import io.nextpos.calendarevent.data.CalendarEvent;
import io.nextpos.calendarevent.data.CalendarEventRepository;
import io.nextpos.calendarevent.data.CalendarEventSeries;
import io.nextpos.calendarevent.data.CalendarEventSeriesRepository;
import io.nextpos.calendarevent.service.bean.EventRepeatObject;
import io.nextpos.calendarevent.service.bean.UpdateCalendarEventObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class EventSeriesUpdaterImpl implements EventSeriesUpdater {

    private final EventSeriesCreator eventSeriesCreator;

    private final CalendarEventRepository calendarEventRepository;

    private final CalendarEventSeriesRepository calendarEventSeriesRepository;

    public EventSeriesUpdaterImpl(EventSeriesCreator eventSeriesCreator, CalendarEventRepository calendarEventRepository, CalendarEventSeriesRepository calendarEventSeriesRepository) {
        this.eventSeriesCreator = eventSeriesCreator;
        this.calendarEventRepository = calendarEventRepository;
        this.calendarEventSeriesRepository = calendarEventSeriesRepository;
    }

    @Override
    public List<CalendarEvent> updateCalendarEvent(CalendarEvent calendarEvent, UpdateCalendarEventObject updateCalendarEvent) {

        final UpdateCalendarEventStrategy updateStrategy = this.resolveUpdateCalendarEventStrategy(calendarEvent, updateCalendarEvent);
        return updateStrategy.updateCalendarEvent(calendarEvent, updateCalendarEvent);
    }

    private UpdateCalendarEventStrategy resolveUpdateCalendarEventStrategy(CalendarEvent calendarEvent, UpdateCalendarEventObject updateCalendarEvent) {

        final EventRepeatObject eventRepeat = updateCalendarEvent.getEventRepeat();
        if (calendarEvent.getEventSeries() == null) {
            if (eventRepeat.getEventRepeat() == CalendarEventSeries.EventRepeat.NONE) {
                return (event, updateObj) -> List.of(this.calendarEventRepository.save(event));
            } else {
                return (event, updateObj) -> this.eventSeriesCreator.createEventSeriesEvent(calendarEvent, eventRepeat);
            }
        } else {
            if (eventRepeat.getEventRepeat() == CalendarEventSeries.EventRepeat.NONE) {
                return (event, updateObj) -> {
                    calendarEventRepository.findAllByClientIdAndEventSeries_Id(event.getClientId(), event.getEventSeries().getId()).forEach(e -> {
                        if (!StringUtils.equals(e.getId(), event.getId())) {
                            calendarEventRepository.delete(e);
                        }
                    });

                    this.calendarEventSeriesRepository.delete(event.getEventSeries());

                    event.setEventSeries(null);
                    return List.of(this.calendarEventRepository.save(event));
                };
            }

            return new UpdateSeriesEventStrategy(calendarEventRepository);
        }
    }

    private static class UpdateSeriesEventStrategy implements UpdateCalendarEventStrategy {

        private final CalendarEventRepository calendarEventRepository;

        private UpdateSeriesEventStrategy(CalendarEventRepository calendarEventRepository) {
            this.calendarEventRepository = calendarEventRepository;
        }

        @Override
        public List<CalendarEvent> updateCalendarEvent(CalendarEvent calendarEvent, UpdateCalendarEventObject updateCalendarEvent) {

            final CalendarEventSeries eventSeries = calendarEvent.getEventSeries();
            List<CalendarEvent> updatedCalendarEvents = new ArrayList<>();
            final LocalDateTime startTime = updateCalendarEvent.getStartTime();
            final LocalDateTime endTime = updateCalendarEvent.getEndTime();

            if (updateCalendarEvent.isApplyToSeries()) {
                Map<LocalDate, CalendarEvent> eventsByDate = calendarEventRepository.findAllByClientIdAndEventSeries_Id(calendarEvent.getClientId(), eventSeries.getId()).stream()
                        .collect(Collectors.toMap(CalendarEvent::getLocalStartDate, e -> e));

                eventSeries.updateAndGetSeriesDates(calendarEvent, updateCalendarEvent.getEventRepeat(), startTime).forEach(date -> {
                    final CalendarEvent event = eventsByDate.get(date);

                    if (event != null) {
                        event.update(calendarEvent, startTime.toLocalTime(), endTime.toLocalTime(), updateCalendarEvent.getDaysDiff());

                        if (!CollectionUtils.isEmpty(updateCalendarEvent.getEventResources())) {
                            event.removeAllEventResources();
                            updateCalendarEvent.getEventResources().forEach(event::addEventSource);
                        }

                        eventsByDate.remove(date);
                        updatedCalendarEvents.add(this.calendarEventRepository.save(event));
                        
                    } else {
                        LocalDateTime newStartTime = LocalDateTime.of(date, startTime.toLocalTime());
                        LocalDateTime newEndTime = LocalDateTime.of(date, endTime.toLocalTime());

                        final CalendarEvent newEvent = calendarEvent.copy(newStartTime, newEndTime);
                        updatedCalendarEvents.add(this.calendarEventRepository.save(newEvent));
                    }
                });

                eventsByDate.values().forEach(calendarEventRepository::delete);

            } else {
                final boolean dateChanged = !calendarEvent.getStartTime().toInstant().equals(startTime.atZone(calendarEvent.getZoneId()).toInstant());

                if (dateChanged) {
                    calendarEvent.setEventSeries(null);
                }

                updatedCalendarEvents.add(this.calendarEventRepository.save(calendarEvent));
            }

            return updatedCalendarEvents;
        }
    }

    @FunctionalInterface
    private interface UpdateCalendarEventStrategy {
        List<CalendarEvent> updateCalendarEvent(CalendarEvent calendarEvent, UpdateCalendarEventObject updateCalendarEvent);
    }

}
