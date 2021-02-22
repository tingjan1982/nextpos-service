package io.nextpos.calendarevent.service.bean;

import io.nextpos.calendarevent.data.CalendarEvent;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class UpdateCalendarEventObject {

    private EventRepeatObject eventRepeat;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
    
    private long daysDiff;

    private boolean applyToSeries;

    private List<CalendarEvent.EventResource> eventResources;
}
