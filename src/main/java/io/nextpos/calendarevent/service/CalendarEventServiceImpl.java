package io.nextpos.calendarevent.service;

import io.nextpos.calendarevent.data.CalendarEvent;
import io.nextpos.calendarevent.data.CalendarEventRepository;
import io.nextpos.client.data.Client;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import io.nextpos.shared.util.DateTimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@ChainedTransaction
public class CalendarEventServiceImpl implements CalendarEventService {

    private final CalendarEventRepository calendarEventRepository;

    @Autowired
    public CalendarEventServiceImpl(CalendarEventRepository calendarEventRepository) {
        this.calendarEventRepository = calendarEventRepository;
    }

    @Override
    public CalendarEvent saveCalendarEvent(CalendarEvent calendarEvent) {
        return calendarEventRepository.save(calendarEvent);
    }

    @Override
    public CalendarEvent updateEventResource(CalendarEvent calendarEvent, CalendarEvent.EventResource eventResource) {

        calendarEvent.updateEventSource(eventResource);
        return saveCalendarEvent(calendarEvent);
    }

    @Override
    public CalendarEvent removeEventResource(CalendarEvent calendarEvent) {
        calendarEvent.updateEventSource(new CalendarEvent.EventResource());

        return saveCalendarEvent(calendarEvent);
    }

    @Override
    public List<CalendarEvent> getCalendarEventsForEventResource(Client client, YearMonth yearMonth, CalendarEvent.EventType eventType, CalendarEvent.EventResource eventResource) {

        final LocalDateTime startOfMonth = yearMonth.atDay(1).atStartOfDay();
        final LocalDateTime endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        return calendarEventRepository.findAllByClientIdAndEventTypeAndEventResource_ResourceIdAndEventResource_ResourceTypeAndStartTimeBetween(
                client.getId(),
                eventType,
                eventResource.getResourceId(),
                eventResource.getResourceType(),
                DateTimeUtil.toDate(client.getZoneId(), startOfMonth),
                DateTimeUtil.toDate(client.getZoneId(), endOfMonth)
        );
    }
}
