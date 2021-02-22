package io.nextpos.calendarevent.service;

import io.nextpos.calendarevent.data.CalendarEvent;
import io.nextpos.calendarevent.service.bean.UpdateCalendarEventObject;

import java.util.List;

public interface EventSeriesUpdater {

    List<CalendarEvent> updateCalendarEvent(CalendarEvent calendarEvent, UpdateCalendarEventObject updateCalendarEvent);
}
