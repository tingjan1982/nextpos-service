package io.nextpos.timecard.web.model;

import io.nextpos.timecard.data.UserTimeCard;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class UserTimeCardResponse {

    private String id;

    private String clientId;

    private String username;

    private String nickname;

    private LocalDateTime clockIn;

    private LocalDateTime clockOut;

    private long hours;

    private long minutes;

    private UserTimeCard.TimeCardStatus timeCardStatus;
}
