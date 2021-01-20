package io.nextpos.calendarevent.data;

import io.nextpos.shared.model.MongoBaseObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CalendarEventSeries extends MongoBaseObject {

    @Id
    private String id;

    private String clientId;

    private EventRepeat eventRepeat;

    public CalendarEventSeries(String clientId, EventRepeat eventRepeat) {
        this.clientId = clientId;
        this.eventRepeat = eventRepeat;
    }

    public enum EventRepeat {
        NONE, DAILY, WEEKLY
    }
}
