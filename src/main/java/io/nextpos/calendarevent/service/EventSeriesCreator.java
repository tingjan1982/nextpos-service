package io.nextpos.calendarevent.service;

import io.nextpos.calendarevent.data.CalendarEvent;
import io.nextpos.calendarevent.service.bean.EventRepeatObject;
import io.nextpos.client.data.Client;

import java.util.List;

public interface EventSeriesCreator {

    List<CalendarEvent> createEventSeriesEvent(Client client, CalendarEvent baseCalendarEvent, EventRepeatObject eventRepeat);
}
