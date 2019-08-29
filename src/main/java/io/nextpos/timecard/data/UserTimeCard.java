package io.nextpos.timecard.data;

import io.nextpos.shared.model.MongoBaseObject;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class UserTimeCard extends MongoBaseObject {

    @Id
    private String id;

    private String clientId;

    private String username;

    private String nickname;

    private TimeCardStatus timeCardStatus;

    private Date clockIn;

    private Date clockOut;

    public UserTimeCard(final String clientId, final String username, final String nickname) {
        this.clientId = clientId;
        this.username = username;
        this.nickname = nickname;
        this.clockIn = new Date();

        timeCardStatus = TimeCardStatus.ACTIVE;
    }

    public void clockOut() {
        this.clockOut = new Date();
        timeCardStatus = TimeCardStatus.COMPLETE;
    }

    @Override
    public boolean isNew() {
        return id == null;
    }


    public enum TimeCardStatus {
        INACTIVE, ACTIVE, COMPLETE
    }
}
