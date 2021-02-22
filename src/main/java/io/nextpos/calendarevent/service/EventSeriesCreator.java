package io.nextpos.calendarevent.service;

import io.nextpos.calendarevent.data.CalendarEvent;
import io.nextpos.calendarevent.service.bean.EventRepeatObject;

import java.util.List;

public interface EventSeriesCreator {

    List<CalendarEvent> createEventSeriesEvent(CalendarEvent baseCalendarEvent, EventRepeatObject eventRepeat);
}
