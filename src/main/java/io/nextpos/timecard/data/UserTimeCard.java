package io.nextpos.timecard.data;

import io.nextpos.calendarevent.data.CalendarEvent;
import io.nextpos.shared.model.MongoBaseObject;
import io.nextpos.shared.model.WithClientId;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class UserTimeCard extends MongoBaseObject implements WithClientId {

    @Id
    private String id;

    private String clientId;

    private String username;

    private String nickname;

    private TimeCardStatus timeCardStatus;

    private Date clockIn;

    private Date clockOut;

    @DBRef
    private CalendarEvent matchedRoster;

    public UserTimeCard(final String clientId, final String username, final String nickname) {
        this.clientId = clientId;
        this.username = username;
        this.nickname = nickname;

        timeCardStatus = TimeCardStatus.INACTIVE;
    }

    public void clockIn() {
        this.clockIn = new Date();
        timeCardStatus = TimeCardStatus.ACTIVE;
    }

    public void clockOut() {
        this.clockOut = new Date();
        timeCardStatus = TimeCardStatus.COMPLETE;
    }

    /**
     * Using system default zone is ok because duration can still be worked out correctly.
     */
    public Duration getWorkingDuration() {

        if (this.clockIn != null && this.clockOut != null) {
            final LocalDateTime clockInDt = LocalDateTime.ofInstant(this.clockIn.toInstant(), ZoneId.systemDefault());
            final LocalDateTime clockOutDt = LocalDateTime.ofInstant(this.clockOut.toInstant(), ZoneId.systemDefault());
            return Duration.between(clockInDt, clockOutDt);
        }

        return Duration.ZERO;
    }

    public enum TimeCardStatus {
        INACTIVE, ACTIVE, COMPLETE
    }
}
