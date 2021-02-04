package io.nextpos.calendarevent.data;

import io.nextpos.shared.model.MongoBaseObject;
import io.nextpos.shared.util.DateTimeUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CalendarEventSeries extends MongoBaseObject {

    @Id
    private String id;

    private String clientId;

    private ZoneId zoneId;

    private EventRepeat eventRepeat;

    private Date repeatEndDate;

    public CalendarEventSeries(String clientId, ZoneId zoneId, EventRepeat eventRepeat, LocalDateTime repeatEndDate) {
        this.clientId = clientId;
        this.zoneId = zoneId;
        this.eventRepeat = eventRepeat;
        this.repeatEndDate = DateTimeUtil.toDate(zoneId, repeatEndDate);
    }

    public enum EventRepeat {
        NONE, DAILY, WEEKLY
    }
}
