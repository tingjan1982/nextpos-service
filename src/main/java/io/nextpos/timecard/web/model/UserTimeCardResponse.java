package io.nextpos.timecard.web.model;

import io.nextpos.calendarevent.data.CalendarEvent;
import io.nextpos.timecard.data.UserTimeCard;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Duration;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Data
@AllArgsConstructor
public class UserTimeCardResponse {

    private String id;

    /**
     * todo: Remove in the future when frontend remove its use.
     */
    @Deprecated
    private String username;

    /**
     * todo: Remove in the future when frontend remove its use.
     */
    @Deprecated
    private String nickname;

    private String displayName;

    private Date clockIn;

    private Date clockOut;

    private UserTimeCard.TimeCardStatus timeCardStatus;

    private long hours;

    private long minutes;

    private Long arriveLateMinutes;

    private Long leaveEarlyMinutes;

    public UserTimeCardResponse(UserTimeCard userTimeCard) {
        id = userTimeCard.getId();
        username = userTimeCard.getUsername();
        nickname = userTimeCard.getNickname();
        displayName = userTimeCard.getNickname();
        clockIn = userTimeCard.getClockIn();
        clockOut = userTimeCard.getClockOut();
        timeCardStatus = userTimeCard.getTimeCardStatus();

        Duration workingDuration = userTimeCard.getWorkingDuration();
        hours = workingDuration.toHours();
        minutes = workingDuration.toMinutesPart();

        final CalendarEvent matchedRoster = userTimeCard.getMatchedRoster();

        if (timeCardStatus == UserTimeCard.TimeCardStatus.COMPLETE && matchedRoster != null) {
            final long startDiff = clockIn.getTime() - matchedRoster.getStartTime().getTime();

            if (startDiff > 0) {
                arriveLateMinutes = TimeUnit.MILLISECONDS.toMinutes(startDiff);
            }

            final long endDiff = matchedRoster.getEndTime().getTime() - clockOut.getTime();

            if (endDiff > 0) {
                leaveEarlyMinutes = TimeUnit.MILLISECONDS.toMinutes(endDiff);
            }
        }
    }
}
