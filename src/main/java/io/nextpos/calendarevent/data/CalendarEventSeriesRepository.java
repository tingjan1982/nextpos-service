package io.nextpos.calendarevent.data;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface CalendarEventSeriesRepository extends MongoRepository<CalendarEventSeries, String> {
}
