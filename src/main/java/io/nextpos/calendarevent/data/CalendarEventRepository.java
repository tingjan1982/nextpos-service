package io.nextpos.calendarevent.data;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;
import java.util.List;

public interface CalendarEventRepository extends MongoRepository<CalendarEvent, String> {

    List<CalendarEvent> findAllByClientIdAndEventTypeAndStartTimeBetween(String clientId, CalendarEvent.EventType eventType, Date from, Date to);

    List<CalendarEvent> findAllByClientIdAndEventSeries_Id(String clientId, String eventSeriesId);
}
