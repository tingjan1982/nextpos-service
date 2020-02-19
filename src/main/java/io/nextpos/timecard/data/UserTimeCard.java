package io.nextpos.timecard.data;

import io.nextpos.shared.model.MongoBaseObject;
import io.nextpos.shared.model.WithClientId;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Duration;
import java.time.LocalDateTime;

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

    private LocalDateTime clockIn;

    private LocalDateTime clockOut;

    public UserTimeCard(final String clientId, final String username, final String nickname) {
        this.clientId = clientId;
        this.username = username;
        this.nickname = nickname;

        timeCardStatus = TimeCardStatus.INACTIVE;
    }

    public void clockIn() {
        this.clockIn = LocalDateTime.now();
        timeCardStatus = TimeCardStatus.ACTIVE;
    }

    public void clockOut() {
        this.clockOut = LocalDateTime.now();
        timeCardStatus = TimeCardStatus.COMPLETE;
    }

    public Duration getWorkingDuration() {

        if (this.clockIn != null && this.clockOut != null) {
            return Duration.between(this.clockIn, this.clockOut);
        }

        return Duration.ZERO;
    }

    @Override
    public boolean isNew() {
        return id == null;
    }


    public enum TimeCardStatus {
        INACTIVE, ACTIVE, COMPLETE
    }
}
