package io.nextpos.roster.service;

import io.nextpos.calendarevent.data.CalendarEvent;
import io.nextpos.calendarevent.data.CalendarEventSeries;
import io.nextpos.client.data.Client;

import java.time.LocalDateTime;
import java.util.List;

public interface EventSeriesCreator {

    List<CalendarEvent> createEventSeriesEvent(Client client, CalendarEvent baseCalendarEvent, CalendarEventSeries.EventRepeat eventRepeat, LocalDateTime repeatEndDate);
}
